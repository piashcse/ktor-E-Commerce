package com.piashcse.feature.analytics

import com.piashcse.constants.AnalyticsEventType
import com.piashcse.constants.Localization
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.AnalyticsEventDAO
import com.piashcse.database.entities.AnalyticsEventTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
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
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime

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
data class AnalyticsBatchRequest(val events: List<AnalyticsEventRequest>) {
    init {
        if (events.isEmpty()) throw ValidationException("At least one event is required")
        if (events.size > 50) throw ValidationException("At most 50 events can be ingested per request")
    }
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
            events.forEach { event ->
                AnalyticsEventDAO.new {
                    this.sessionId = event.sessionId
                    this.userId = userId?.entityID(UserTable)
                    this.eventType = event.eventType
                    this.productId = event.productId?.entityID(com.piashcse.database.entities.ProductTable)
                    this.orderId = event.orderId?.entityID(com.piashcse.database.entities.OrderTable)
                    this.path = event.path
                    this.referrer = event.referrer
                    this.utmSource = event.utmSource
                    this.utmMedium = event.utmMedium
                    this.utmCampaign = event.utmCampaign
                    this.utmTerm = event.utmTerm
                    this.utmContent = event.utmContent
                    this.locale = event.locale?.let { Localization.normalize(it) }
                    this.value = event.value
                    this.currency = event.currency?.uppercase()
                    this.metadata = event.metadata
                    this.occurredAt = event.occurredAt?.let(::parseDate) ?: now
                }
            }
            AnalyticsIngestResponse(accepted = events.size)
        }

    suspend fun funnel(
        startDate: String?,
        endDate: String?,
    ): FunnelResponse =
        query {
            val (start, end) = range(startDate, endDate)
            val events =
                AnalyticsEventTable.selectAll().where {
                    (AnalyticsEventTable.occurredAt greaterEq start) and
                        (AnalyticsEventTable.occurredAt lessEq end)
                }.toList()
            val sessions =
                mapOf(
                    AnalyticsEventType.PAGE_VIEW to
                        events.filter { it[AnalyticsEventTable.eventType] == AnalyticsEventType.PAGE_VIEW }
                            .map { it[AnalyticsEventTable.sessionId] }.toSet(),
                    AnalyticsEventType.PRODUCT_VIEW to
                        events.filter { it[AnalyticsEventTable.eventType] == AnalyticsEventType.PRODUCT_VIEW }
                            .map { it[AnalyticsEventTable.sessionId] }.toSet(),
                    AnalyticsEventType.ADD_TO_CART to
                        events.filter { it[AnalyticsEventTable.eventType] == AnalyticsEventType.ADD_TO_CART }
                            .map { it[AnalyticsEventTable.sessionId] }.toSet(),
                    AnalyticsEventType.CHECKOUT_START to
                        events.filter { it[AnalyticsEventTable.eventType] == AnalyticsEventType.CHECKOUT_START }
                            .map { it[AnalyticsEventTable.sessionId] }.toSet(),
                )
            val trackedSessions = events.map { it[AnalyticsEventTable.sessionId] }.toSet()
            val purchases =
                if (trackedSessions.isEmpty()) {
                    0L
                } else {
                    OrderTable.selectAll().where {
                        (OrderTable.paymentStatus eq PaymentStatus.COMPLETED) and
                            (OrderTable.status neq OrderStatus.CANCELED) and
                            (OrderTable.createdAt greaterEq start) and
                            (OrderTable.createdAt lessEq end) and
                            (OrderTable.attributionSessionId inList trackedSessions)
                    }.count()
                }

            val counts =
                listOf(
                    "page_view" to sessions.getValue(AnalyticsEventType.PAGE_VIEW).size.toLong(),
                    "product_view" to sessions.getValue(AnalyticsEventType.PRODUCT_VIEW).size.toLong(),
                    "add_to_cart" to sessions.getValue(AnalyticsEventType.ADD_TO_CART).size.toLong(),
                    "checkout_start" to sessions.getValue(AnalyticsEventType.CHECKOUT_START).size.toLong(),
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
        query {
            val (start, end) = range(startDate, endDate)
            val orders =
                OrderTable.selectAll().where {
                    (OrderTable.paymentStatus eq PaymentStatus.COMPLETED) and
                        (OrderTable.status neq OrderStatus.CANCELED) and
                        (OrderTable.createdAt greaterEq start) and
                        (OrderTable.createdAt lessEq end)
                }.map { OrderDAO.wrapRow(it) }
            val rows =
                orders.groupBy {
                    Triple(
                        it.attributionSource ?: "direct",
                        it.attributionMedium ?: "none",
                        it.attributionCampaign ?: "none",
                    )
                }.map { (key, group) ->
                    val revenueUsd =
                        group.fold(BigDecimal.ZERO) { acc, order ->
                            acc.add(toUsd(order.total, order.fxRate))
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
        query {
            val (start, end) = range(startDate, endDate)
            val events =
                AnalyticsEventTable.selectAll().where {
                    (AnalyticsEventTable.occurredAt greaterEq start) and
                        (AnalyticsEventTable.occurredAt lessEq end) and
                        (AnalyticsEventTable.productId.isNotNull())
                }.toList()
            val views =
                events.filter { it[AnalyticsEventTable.eventType] == AnalyticsEventType.PRODUCT_VIEW }
                    .groupBy { it[AnalyticsEventTable.productId]!!.value }
                    .mapValues { (_, rows) -> rows.map { it[AnalyticsEventTable.sessionId] }.toSet().size.toLong() }
            val carts =
                events.filter { it[AnalyticsEventTable.eventType] == AnalyticsEventType.ADD_TO_CART }
                    .groupBy { it[AnalyticsEventTable.productId]!!.value }
                    .mapValues { (_, rows) -> rows.map { it[AnalyticsEventTable.sessionId] }.toSet().size.toLong() }

            val purchases = mutableMapOf<String, Long>()
            val revenue = mutableMapOf<String, BigDecimal>()
            OrderTable.selectAll().where {
                (OrderTable.paymentStatus eq PaymentStatus.COMPLETED) and
                    (OrderTable.status neq OrderStatus.CANCELED) and
                    (OrderTable.createdAt greaterEq start) and
                    (OrderTable.createdAt lessEq end)
            }.map { OrderDAO.wrapRow(it) }.forEach { order ->
                OrderItemDAO.itemsForOrder(order.id).forEach { item ->
                    val key = item.productId.value
                    purchases[key] = (purchases[key] ?: 0L) + item.quantity
                    revenue[key] = (revenue[key] ?: BigDecimal.ZERO).add(toUsd(item.total, order.fxRate))
                }
            }

            val productIds = (views.keys + carts.keys + purchases.keys).toSet()
            val rows =
                productIds.map { productId ->
                    val viewCount = views[productId] ?: 0L
                    val cartCount = carts[productId] ?: 0L
                    val purchaseCount = purchases[productId] ?: 0L
                    ProductFunnelRow(
                        productId = productId,
                        name = ProductDAO.findById(productId)?.name ?: productId,
                        views = viewCount,
                        addToCart = cartCount,
                        purchases = purchaseCount,
                        conversionRate = if (viewCount == 0L) 0.0 else percentage(purchaseCount, viewCount),
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
            startDate?.let { LocalDate.parse(it).atStartOfDay() }
                ?: now.minusDays(30).toLocalDate().atStartOfDay()
        val end = endDate?.let { LocalDate.parse(it).atTime(LocalTime.MAX) } ?: now
        if (start.isAfter(end)) throw ValidationException("startDate must be before endDate")
        return start to end
    }

    private fun parseDate(value: String): LocalDateTime =
        runCatching { LocalDateTime.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).toLocalDateTime() }
            .getOrElse { throw ValidationException("Invalid occurredAt: $value") }

    private fun toUsd(
        amount: BigDecimal,
        fxRate: BigDecimal,
    ): BigDecimal = if (fxRate <= BigDecimal.ZERO) BigDecimal.ZERO.setScale(2) else amount.divide(fxRate, 2, RoundingMode.HALF_UP)

    private fun percentage(
        part: Long,
        whole: Long,
    ): Double = ((part.toDouble() / whole.toDouble()) * 100.0 * 100.0).roundToLong() / 100.0

    private fun Double.roundToLong(): Double = kotlin.math.round(this)
}
