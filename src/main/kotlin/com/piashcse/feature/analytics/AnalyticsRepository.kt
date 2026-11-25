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
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.readQuery
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.select
import java.math.BigDecimal
import java.time.LocalDateTime

data class AnalyticsAttributionOrder(
    val source: String?,
    val medium: String?,
    val campaign: String?,
    val total: BigDecimal,
    val fxRate: BigDecimal,
)

data class AnalyticsProductEvent(
    val productId: String,
    val eventType: AnalyticsEventType,
    val sessionId: String,
)

data class AnalyticsOrderRef(
    val id: String,
    val fxRate: BigDecimal,
)

data class AnalyticsOrderItem(
    val productId: String,
    val orderId: String,
    val quantity: Int,
    val total: BigDecimal,
)

interface AnalyticsRepository {
    suspend fun track(
        userId: String?,
        events: List<AnalyticsEventRequest>,
    ): AnalyticsIngestResponse

    suspend fun funnelSessions(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Map<AnalyticsEventType, Set<String>>

    suspend fun purchaseSessions(
        start: LocalDateTime,
        end: LocalDateTime,
        trackedSessions: Set<String>,
    ): Long

    suspend fun attributionOrders(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<AnalyticsAttributionOrder>

    suspend fun productEvents(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<AnalyticsProductEvent>

    suspend fun completedOrders(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<AnalyticsOrderRef>

    suspend fun orderItems(orderIds: List<String>): List<AnalyticsOrderItem>

    suspend fun productNames(productIds: Set<String>): Map<String, String>
}

class AnalyticsRepositoryImpl : AnalyticsRepository {
    override suspend fun track(
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
                    this.productId = event.productId?.entityID(ProductTable)
                    this.orderId = event.orderId?.entityID(OrderTable)
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

    override suspend fun funnelSessions(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Map<AnalyticsEventType, Set<String>> =
        readQuery {
            listOf(
                AnalyticsEventType.PAGE_VIEW,
                AnalyticsEventType.PRODUCT_VIEW,
                AnalyticsEventType.ADD_TO_CART,
                AnalyticsEventType.CHECKOUT_START,
            ).associateWith { type ->
                AnalyticsEventTable.select(AnalyticsEventTable.sessionId)
                    .withDistinct()
                    .where {
                        (AnalyticsEventTable.occurredAt greaterEq start) and
                            (AnalyticsEventTable.occurredAt lessEq end) and
                            (AnalyticsEventTable.eventType eq type)
                    }
                    .map { it[AnalyticsEventTable.sessionId] }
                    .toSet()
            }
        }

    override suspend fun purchaseSessions(
        start: LocalDateTime,
        end: LocalDateTime,
        trackedSessions: Set<String>,
    ): Long =
        readQuery {
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
        }

    override suspend fun attributionOrders(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<AnalyticsAttributionOrder> =
        readQuery {
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
                }.map {
                    AnalyticsAttributionOrder(
                        source = it[OrderTable.attributionSource],
                        medium = it[OrderTable.attributionMedium],
                        campaign = it[OrderTable.attributionCampaign],
                        total = it[OrderTable.total],
                        fxRate = it[OrderTable.fxRate],
                    )
                }
        }

    override suspend fun productEvents(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<AnalyticsProductEvent> =
        readQuery {
            AnalyticsEventTable
                .select(
                    AnalyticsEventTable.productId,
                    AnalyticsEventTable.eventType,
                    AnalyticsEventTable.sessionId,
                ).where {
                    (AnalyticsEventTable.occurredAt greaterEq start) and
                        (AnalyticsEventTable.occurredAt lessEq end) and
                        (AnalyticsEventTable.productId.isNotNull())
                }.mapNotNull { row ->
                    row[AnalyticsEventTable.productId]?.value?.let { productId ->
                        AnalyticsProductEvent(
                            productId = productId,
                            eventType = row[AnalyticsEventTable.eventType],
                            sessionId = row[AnalyticsEventTable.sessionId],
                        )
                    }
                }
        }

    override suspend fun completedOrders(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<AnalyticsOrderRef> =
        readQuery {
            OrderTable.select(OrderTable.id, OrderTable.fxRate).where {
                (OrderTable.paymentStatus eq PaymentStatus.COMPLETED) and
                    (OrderTable.status neq OrderStatus.CANCELED) and
                    (OrderTable.createdAt greaterEq start) and
                    (OrderTable.createdAt lessEq end)
            }.map {
                AnalyticsOrderRef(
                    id = it[OrderTable.id].value,
                    fxRate = it[OrderTable.fxRate],
                )
            }
        }

    override suspend fun orderItems(orderIds: List<String>): List<AnalyticsOrderItem> =
        readQuery {
            if (orderIds.isEmpty()) {
                emptyList()
            } else {
                OrderItemDAO.find { OrderItemTable.orderId inList orderIds.map { it.entityID(OrderTable) } }
                    .map {
                        AnalyticsOrderItem(
                            productId = it.productId.value,
                            orderId = it.orderId.value,
                            quantity = it.quantity,
                            total = it.total,
                        )
                    }
            }
        }

    override suspend fun productNames(productIds: Set<String>): Map<String, String> =
        readQuery {
            if (productIds.isEmpty()) {
                emptyMap()
            } else {
                ProductDAO.find { ProductTable.id inList productIds.map { it.entityID(ProductTable) } }
                    .associate { it.id.value to it.name }
            }
        }
}
