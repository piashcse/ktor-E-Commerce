package com.piashcse.feature.analytics

import com.piashcse.constants.AnalyticsEventType
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.DateTimes
import com.piashcse.utils.common.Money
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime

@Serializable
data class AnalyticsEventRequest(
    val eventType: AnalyticsEventType,
    val sessionId: String,
    val productId: String? = null,
    val orderId: String? = null,
    val path: String? = null,
    val referrer: String? = null,
    val utmSource: String? = null,
    val utmMedium: String? = null,
    val utmCampaign: String? = null,
    val utmTerm: String? = null,
    val utmContent: String? = null,
    val locale: String? = null,
    @Contextual val value: BigDecimal? = null,
    val currency: String? = null,
    val metadata: String? = null,
    val occurredAt: String? = null,
) {
    init {
        if (sessionId.isBlank()) throw ValidationException("sessionId is required")
    }
}

@Serializable
data class AnalyticsBatchRequest(
    val events: List<AnalyticsEventRequest>,
    /** Explicit consent flag; ingestion is skipped when false/omitted. */
    val consent: Boolean = false,
) {
    init {
        if (events.isEmpty()) throw ValidationException("At least one event is required")
        if (events.size > 50) throw ValidationException("At most 50 events can be ingested per request")
    }
}

/** Consent and bot screening for public analytics ingestion. */
object AnalyticsIngestPolicy {
    private val botPattern =
        Regex(
            "(?i)(bot|crawler|spider|slurp|bingpreview|facebookexternalhit|headlesschrome|" +
                "python-requests|python-urllib|curl/|wget|scrapy)",
        )

    fun shouldIngest(
        consent: Boolean,
        userAgent: String?,
    ): Boolean = consent && !botPattern.containsMatchIn(userAgent.orEmpty())
}

@Serializable
data class AnalyticsIngestResponse(
    val accepted: Int,
    val rejected: Int = 0,
)

@Serializable
data class FunnelStage(
    val name: String,
    val sessions: Long,
    val conversionFromPrevious: Double?,
)

@Serializable
data class FunnelResponse(
    val startDate: String,
    val endDate: String,
    val stages: List<FunnelStage>,
    val overallConversion: Double,
)

@Serializable
data class AttributionRow(
    val source: String,
    val medium: String,
    val campaign: String,
    val orders: Long,
    val revenue: String,
    val revenueUsd: String,
)

@Serializable
data class AttributionResponse(
    val startDate: String,
    val endDate: String,
    val rows: List<AttributionRow>,
    val totalOrders: Long,
    val totalRevenueUsd: String,
)

@Serializable
data class ProductFunnelRow(
    val productId: String,
    val name: String,
    val views: Long,
    val addToCart: Long,
    val purchases: Long,
    val conversionRate: Double,
    val revenue: String,
)

@Serializable
data class ProductFunnelResponse(
    val startDate: String,
    val endDate: String,
    val products: List<ProductFunnelRow>,
)

class AnalyticsService(
    private val repository: AnalyticsRepository,
) {
    suspend fun track(
        userId: String?,
        events: List<AnalyticsEventRequest>,
    ): AnalyticsIngestResponse = repository.track(userId, events)

    suspend fun funnel(
        startDate: String?,
        endDate: String?,
    ): FunnelResponse {
        val (start, end) = range(startDate, endDate)
        val sessions = repository.funnelSessions(start, end)

        // Ordered funnel: each stage must contain the prior stage's sessions.
        // Independent sets can produce conversions above 100%.
        val pageSessions = sessions[AnalyticsEventType.PAGE_VIEW].orEmpty()
        val productSessions = sessions[AnalyticsEventType.PRODUCT_VIEW].orEmpty().intersect(pageSessions)
        val cartSessions = sessions[AnalyticsEventType.ADD_TO_CART].orEmpty().intersect(productSessions)
        val checkoutSessions = sessions[AnalyticsEventType.CHECKOUT_START].orEmpty().intersect(cartSessions)
        val trackedSessions =
            pageSessions + productSessions + cartSessions + checkoutSessions
        val purchases = repository.purchaseSessions(start, end, trackedSessions)

        val counts =
            listOf(
                "page_view" to pageSessions.size.toLong(),
                "product_view" to productSessions.size.toLong(),
                "add_to_cart" to cartSessions.size.toLong(),
                "checkout_start" to checkoutSessions.size.toLong(),
                "purchase" to purchases,
            )
        val stages =
            counts.mapIndexed { index, (name, count) ->
                val previous = counts.getOrNull(index - 1)?.second
                FunnelStage(
                    name = name,
                    sessions = count,
                    conversionFromPrevious =
                        if (previous == null || previous == 0L) null else percentage(count, previous),
                )
            }
        val first = counts.first().second
        return FunnelResponse(
            startDate = start.toLocalDate().toString(),
            endDate = end.toLocalDate().toString(),
            stages = stages,
            overallConversion = if (first == 0L) 0.0 else percentage(counts.last().second, first),
        )
    }

    suspend fun attribution(
        startDate: String?,
        endDate: String?,
    ): AttributionResponse {
        val (start, end) = range(startDate, endDate)
        val rows =
            repository.attributionOrders(start, end)
                .groupBy {
                    Triple(
                        it.source ?: "direct",
                        it.medium ?: "none",
                        it.campaign ?: "none",
                    )
                }.map { (key, group) ->
                    val revenueUsd =
                        group.fold(BigDecimal.ZERO) { acc, order ->
                            acc.add(Money.toUsd(order.total, order.fxRate))
                        }
                    AttributionRow(
                        source = key.first,
                        medium = key.second,
                        campaign = key.third,
                        orders = group.size.toLong(),
                        revenue = Money.str(group.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.total) }),
                        revenueUsd = Money.str(revenueUsd),
                    )
                }.sortedByDescending { it.revenueUsd.toBigDecimalOrNull() ?: BigDecimal.ZERO }
        return AttributionResponse(
            startDate = start.toLocalDate().toString(),
            endDate = end.toLocalDate().toString(),
            rows = rows,
            totalOrders = rows.sumOf { it.orders },
            totalRevenueUsd = Money.str(rows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.revenueUsd.toBigDecimal()) }),
        )
    }

    suspend fun productFunnel(
        startDate: String?,
        endDate: String?,
        limit: Int,
    ): ProductFunnelResponse {
        val (start, end) = range(startDate, endDate)
        val events = repository.productEvents(start, end)
        val views =
            events.filter { it.eventType == AnalyticsEventType.PRODUCT_VIEW }
                .groupBy({ it.productId }, { it.sessionId })
                .mapValues { (_, sessions) -> sessions.toSet().size.toLong() }
        val carts =
            events.filter { it.eventType == AnalyticsEventType.ADD_TO_CART }
                .groupBy({ it.productId }, { it.sessionId })
                .mapValues { (_, sessions) -> sessions.toSet().size.toLong() }

        val orders = repository.completedOrders(start, end)
        val fxByOrder = orders.associate { it.id to it.fxRate }
        val items = repository.orderItems(orders.map { it.id })
        val purchases = mutableMapOf<String, Long>()
        val purchaseOrders = mutableMapOf<String, MutableSet<String>>()
        val revenue = mutableMapOf<String, BigDecimal>()
        items.forEach { item ->
            purchases[item.productId] = (purchases[item.productId] ?: 0L) + item.quantity
            purchaseOrders.getOrPut(item.productId) { mutableSetOf() }.add(item.orderId)
            revenue[item.productId] =
                (revenue[item.productId] ?: BigDecimal.ZERO)
                    .add(Money.toUsd(item.total, fxByOrder[item.orderId] ?: BigDecimal.ONE))
        }

        val productIds = (views.keys + carts.keys + purchases.keys).toSet()
        val names = repository.productNames(productIds)
        val rows =
            productIds.map { productId ->
                val viewCount = views[productId] ?: 0L
                val cartCount = carts[productId] ?: 0L
                val purchaseCount = purchases[productId] ?: 0L
                val purchaseOrderCount = (purchaseOrders[productId]?.size ?: 0).toLong()
                ProductFunnelRow(
                    productId = productId,
                    name = names[productId] ?: productId,
                    views = viewCount,
                    addToCart = cartCount,
                    purchases = purchaseCount,
                    conversionRate =
                        if (viewCount == 0L) {
                            0.0
                        } else {
                            percentage(purchaseOrderCount.coerceAtMost(viewCount), viewCount)
                        },
                    revenue = Money.str(revenue[productId] ?: BigDecimal.ZERO),
                )
            }.sortedWith(compareByDescending<ProductFunnelRow> { it.purchases }.thenByDescending { it.views })
                .take(limit.coerceIn(1, 100))
        return ProductFunnelResponse(start.toLocalDate().toString(), end.toLocalDate().toString(), rows)
    }

    private fun range(
        startDate: String?,
        endDate: String?,
    ): Pair<LocalDateTime, LocalDateTime> {
        val now = currentUtc()
        val start =
            startDate?.let { DateTimes.parseLocalDate(it, "startDate").atStartOfDay() }
                ?: now.minusDays(30).toLocalDate().atStartOfDay()
        val end = endDate?.let { DateTimes.parseLocalDate(it, "endDate").atTime(LocalTime.MAX) } ?: now
        if (start.isAfter(end)) throw ValidationException("startDate must be before endDate")
        return start to end
    }

    private fun percentage(
        part: Long,
        whole: Long,
    ): Double = ((part.toDouble() / whole.toDouble()) * 100.0 * 100.0).roundToLong() / 100.0

    private fun Double.roundToLong(): Double = kotlin.math.round(this)
}
