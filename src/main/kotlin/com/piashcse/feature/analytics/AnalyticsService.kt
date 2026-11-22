package com.piashcse.feature.analytics

import com.piashcse.constants.AnalyticsEventType
import com.piashcse.constants.Localization
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.AnalyticsEventDAO
import com.piashcse.database.entities.AnalyticsEventTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.OrderItemTable
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.DateTimes
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.readQuery
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
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

class AnalyticsService {
    suspend fun track(
        userId: String?,
        events: List<AnalyticsEventRequest>,
    ): AnalyticsIngestResponse =
        query {
            val now = currentUtc()
            var accepted = 0
            var rejected = 0
            events.forEach { event ->
                // Do not let a public caller probe or insert dangling foreign
                // keys: unknown product/order references are rejected quietly.
                val productOk =
                    event.productId == null ||
                        ProductDAO.findById(event.productId) != null
                val orderOk =
                    event.orderId == null ||
                        OrderDAO.findById(event.orderId) != null
                if (!productOk || !orderOk || event.sessionId.length > 100) {
                    rejected++
                    return@forEach
                }
                AnalyticsEventDAO.new {
                    this.sessionId = event.sessionId
                    this.userId = userId?.entityID(UserTable)
                    this.eventType = event.eventType
                    this.productId = event.productId?.entityID(com.piashcse.database.entities.ProductTable)
                    this.orderId = event.orderId?.entityID(com.piashcse.database.entities.OrderTable)
                    this.path = event.path?.take(2_000)
                    this.referrer = event.referrer?.take(2_000)
                    this.utmSource = event.utmSource?.take(255)
                    this.utmMedium = event.utmMedium?.take(255)
                    this.utmCampaign = event.utmCampaign?.take(255)
                    this.utmTerm = event.utmTerm?.take(255)
                    this.utmContent = event.utmContent?.take(255)
                    this.locale = event.locale?.let { Localization.normalize(it) }
                    this.value = event.value
                    this.currency = event.currency?.uppercase()?.take(3)
                    this.metadata = event.metadata?.take(10_000)
                    this.occurredAt = DateTimes.parseFlexibleOrNull(event.occurredAt, "occurredAt") ?: now
                }
                accepted++
            }
            AnalyticsIngestResponse(accepted = accepted, rejected = rejected)
        }

    suspend fun funnel(
        startDate: String?,
        endDate: String?,
    ): FunnelResponse =
        readQuery {
            val (start, end) = range(startDate, endDate)

            fun sessionsFor(type: AnalyticsEventType): Set<String> =
                AnalyticsEventTable.select(AnalyticsEventTable.sessionId)
                    .withDistinct()
                    .where {
                        (AnalyticsEventTable.occurredAt greaterEq start) and
                            (AnalyticsEventTable.occurredAt lessEq end) and
                            (AnalyticsEventTable.eventType eq type)
                    }
                    .map { it[AnalyticsEventTable.sessionId] }
                    .toSet()

            val sessions =
                mapOf(
                    AnalyticsEventType.PAGE_VIEW to sessionsFor(AnalyticsEventType.PAGE_VIEW),
                    AnalyticsEventType.PRODUCT_VIEW to sessionsFor(AnalyticsEventType.PRODUCT_VIEW),
                    AnalyticsEventType.ADD_TO_CART to sessionsFor(AnalyticsEventType.ADD_TO_CART),
                    AnalyticsEventType.CHECKOUT_START to sessionsFor(AnalyticsEventType.CHECKOUT_START),
                )
            // Ordered funnel: each stage must contain the prior stage's
            // sessions. Independent sets can produce conversions above 100%.
            val pageSessions = sessions.getValue(AnalyticsEventType.PAGE_VIEW)
            val productSessions = sessions.getValue(AnalyticsEventType.PRODUCT_VIEW).intersect(pageSessions)
            val cartSessions = sessions.getValue(AnalyticsEventType.ADD_TO_CART).intersect(productSessions)
            val checkoutSessions = sessions.getValue(AnalyticsEventType.CHECKOUT_START).intersect(cartSessions)
            val orderedSessions =
                mapOf(
                    AnalyticsEventType.PAGE_VIEW to pageSessions,
                    AnalyticsEventType.PRODUCT_VIEW to productSessions,
                    AnalyticsEventType.ADD_TO_CART to cartSessions,
                    AnalyticsEventType.CHECKOUT_START to checkoutSessions,
                )
            val trackedSessions = orderedSessions.values.flatten().toSet()
            val purchases =
                if (trackedSessions.isEmpty()) {
                    0L
                } else {
                    OrderTable.select(OrderTable.attributionSessionId)
                        .where {
                            (OrderTable.paymentStatus eq PaymentStatus.COMPLETED) and
                                (OrderTable.status neq OrderStatus.CANCELED) and
                                (OrderTable.createdAt greaterEq start) and
                                (OrderTable.createdAt lessEq end) and
                                (OrderTable.attributionSessionId inList trackedSessions)
                        }
                        .mapNotNull { it[OrderTable.attributionSessionId] }
                        .distinct()
                        .size.toLong()
                }

            val counts =
                listOf(
                    "page_view" to orderedSessions.getValue(AnalyticsEventType.PAGE_VIEW).size.toLong(),
                    "product_view" to orderedSessions.getValue(AnalyticsEventType.PRODUCT_VIEW).size.toLong(),
                    "add_to_cart" to orderedSessions.getValue(AnalyticsEventType.ADD_TO_CART).size.toLong(),
                    "checkout_start" to orderedSessions.getValue(AnalyticsEventType.CHECKOUT_START).size.toLong(),
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
            FunnelResponse(
                startDate = start.toLocalDate().toString(),
                endDate = end.toLocalDate().toString(),
                stages = stages,
                overallConversion = if (first == 0L) 0.0 else percentage(counts.last().second, first),
            )
        }

    suspend fun attribution(
        startDate: String?,
        endDate: String?,
    ): AttributionResponse =
        readQuery {
            val (start, end) = range(startDate, endDate)
            val orders =
                OrderTable
                    .select(
                        OrderTable.attributionSource,
                        OrderTable.attributionMedium,
                        OrderTable.attributionCampaign,
                        OrderTable.total,
                        OrderTable.fxRate,
                    ).where {
                        (OrderTable.paymentStatus eq PaymentStatus.COMPLETED) and
                            (OrderTable.status neq OrderStatus.CANCELED) and
                            (OrderTable.createdAt greaterEq start) and
                            (OrderTable.createdAt lessEq end)
                    }.toList()
            val rows =
                orders.groupBy {
                    Triple(
                        it[OrderTable.attributionSource] ?: "direct",
                        it[OrderTable.attributionMedium] ?: "none",
                        it[OrderTable.attributionCampaign] ?: "none",
                    )
                }.map { (key, group) ->
                    val revenueUsd =
                        group.fold(BigDecimal.ZERO) { acc, order ->
                            acc.add(Money.toUsd(order[OrderTable.total], order[OrderTable.fxRate]))
                        }
                    AttributionRow(
                        source = key.first,
                        medium = key.second,
                        campaign = key.third,
                        orders = group.size.toLong(),
                        revenue = Money.str(group.fold(BigDecimal.ZERO) { acc, order -> acc.add(order[OrderTable.total]) }),
                        revenueUsd = Money.str(revenueUsd),
                    )
                }.sortedByDescending { it.revenueUsd.toBigDecimalOrNull() ?: BigDecimal.ZERO }
            AttributionResponse(
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
    ): ProductFunnelResponse =
        readQuery {
            val (start, end) = range(startDate, endDate)
            val events =
                AnalyticsEventTable
                    .select(
                        AnalyticsEventTable.productId,
                        AnalyticsEventTable.eventType,
                        AnalyticsEventTable.sessionId,
                    ).where {
                        (AnalyticsEventTable.occurredAt greaterEq start) and
                            (AnalyticsEventTable.occurredAt lessEq end) and
                            (AnalyticsEventTable.productId.isNotNull())
                    }.toList()
            val views =
                events.filter { it[AnalyticsEventTable.eventType] == AnalyticsEventType.PRODUCT_VIEW }
                    .mapNotNull { row -> row[AnalyticsEventTable.productId]?.value?.let { it to row } }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, rows) -> rows.map { it[AnalyticsEventTable.sessionId] }.toSet().size.toLong() }
            val carts =
                events.filter { it[AnalyticsEventTable.eventType] == AnalyticsEventType.ADD_TO_CART }
                    .mapNotNull { row -> row[AnalyticsEventTable.productId]?.value?.let { it to row } }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, rows) -> rows.map { it[AnalyticsEventTable.sessionId] }.toSet().size.toLong() }

            val orders =
                OrderTable.selectAll().where {
                    (OrderTable.paymentStatus eq PaymentStatus.COMPLETED) and
                        (OrderTable.status neq OrderStatus.CANCELED) and
                        (OrderTable.createdAt greaterEq start) and
                        (OrderTable.createdAt lessEq end)
                }.map { OrderDAO.wrapRow(it) }
            val fxByOrder = orders.associate { it.id.value to it.fxRate }
            val items =
                if (orders.isEmpty()) {
                    emptyList()
                } else {
                    OrderItemDAO.find { OrderItemTable.orderId inList orders.map { it.id } }.toList()
                }
            val purchases = mutableMapOf<String, Long>()
            val purchaseOrders = mutableMapOf<String, MutableSet<String>>()
            val revenue = mutableMapOf<String, BigDecimal>()
            items.forEach { item ->
                val key = item.productId.value
                purchases[key] = (purchases[key] ?: 0L) + item.quantity
                purchaseOrders.getOrPut(key) { mutableSetOf() }.add(item.orderId.value)
                revenue[key] =
                    (revenue[key] ?: BigDecimal.ZERO)
                        .add(Money.toUsd(item.total, fxByOrder[item.orderId.value] ?: BigDecimal.ONE))
            }

            val productIds = (views.keys + carts.keys + purchases.keys).toSet()
            val names =
                if (productIds.isEmpty()) {
                    emptyMap()
                } else {
                    ProductDAO.find { ProductTable.id inList productIds.map { it.entityID(ProductTable) } }
                        .associate { it.id.value to it.name }
                }
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
            ProductFunnelResponse(start.toLocalDate().toString(), end.toLocalDate().toString(), rows)
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
