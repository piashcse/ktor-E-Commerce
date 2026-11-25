package com.piashcse.feature.omnichannel

import com.piashcse.constants.FulfillmentType
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.PickupStatus
import com.piashcse.constants.PosSessionStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderStatusHistoryDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.PaymentTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.database.entities.WarehouseTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.OutboxAggregate
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.PaymentCompletedEvent
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.order.OrderEvents
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.feature.tax.TaxService
import com.piashcse.mapper.toOrderResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.readQuery
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class StoreResponse(
    val id: String,
    val shopId: String,
    val name: String,
    val address: String?,
    val city: String?,
    val country: String?,
    val latitude: String?,
    val longitude: String?,
    val phone: String?,
    val openingHours: String?,
    val supportsPickup: Boolean,
    val supportsPos: Boolean,
    val distanceKm: Double?,
)

@Serializable
data class PosSaleItem(
    val productId: String,
    val quantity: Int,
    val variantId: String? = null,
) {
    init {
        if (quantity <= 0) throw FieldValidationException(listOf(FieldViolation("quantity", "Quantity must be positive")))
    }
}

@Serializable
data class PosSaleRequest(
    val items: List<PosSaleItem>,
    val warehouseId: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.COD,
    val customerEmail: String? = null,
    val idempotencyKey: String? = null,
    /** Optional open cash-drawer session; COD sales are counted as cash. */
    val posSessionId: String? = null,
) {
    init {
        if (items.isEmpty()) throw FieldValidationException(listOf(FieldViolation("items", "At least one item is required")))
    }
}

@Serializable
data class PickupCompleteRequest(val pickupCode: String)

@Serializable
data class PosSessionOpenRequest(
    @Contextual val openingFloat: BigDecimal = BigDecimal.ZERO,
    val warehouseId: String? = null,
    val notes: String? = null,
) {
    init {
        if (openingFloat < BigDecimal.ZERO) {
            throw FieldValidationException(listOf(FieldViolation("openingFloat", "Opening float cannot be negative")))
        }
    }
}

@Serializable
data class PosSessionCloseRequest(
    @Contextual val countedCash: BigDecimal,
    val notes: String? = null,
) {
    init {
        if (countedCash < BigDecimal.ZERO) {
            throw FieldValidationException(listOf(FieldViolation("countedCash", "Counted cash cannot be negative")))
        }
    }
}

@Serializable
data class PosSessionResponse(
    val id: String,
    val shopId: String,
    val warehouseId: String?,
    val openedBy: String,
    val status: PosSessionStatus,
    val openingFloat: String,
    val cashSales: String,
    val cardSales: String,
    val expectedCash: String?,
    val countedCash: String?,
    val variance: String?,
    val openedAt: String,
    val closedAt: String?,
    val notes: String?,
    val closeNotes: String?,
)

class OmnichannelService(
    private val digitalDelivery: DigitalDeliveryService,
    private val taxService: TaxService,
    private val loyaltyService: LoyaltyService,
) {
    private val pos = PosService(digitalDelivery, taxService, loyaltyService)

    suspend fun stores(
        latitude: Double?,
        longitude: Double?,
        radiusKm: Double?,
        limit: Int,
    ): List<StoreResponse> =
        query {
            val approvedShops =
                ShopDAO.find {
                    ShopTable.status eq ShopStatus.APPROVED
                }.map { it.id }.toSet()
            val rows =
                WarehouseDAO.find {
                    (WarehouseTable.isActive eq true) and
                        (WarehouseTable.supportsPickup eq true) and
                        (WarehouseTable.shopId inList approvedShops)
                }.map { it.toStoreResponse(latitude, longitude) }
            rows.filter { warehouse ->
                radiusKm == null || warehouse.distanceKm == null || warehouse.distanceKm <= radiusKm
            }.sortedWith(compareBy<StoreResponse> { it.distanceKm ?: Double.MAX_VALUE }).take(limit.coerceIn(1, 100))
        }

    suspend fun pickupOrders(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> =
        readQuery {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId?.value ?: throw ValidationException("Seller has no shop")
            OrderTable.selectAll()
                .andWhere {
                    (OrderTable.shopId eq shopId.entityID(ShopTable)) and
                        (OrderTable.fulfillmentType eq FulfillmentType.PICKUP)
                }
                .orderBy(OrderTable.createdAt to SortOrder.DESC)
                .toPosOrdersPaginated(limit, offset)
        }

    suspend fun markReady(
        sellerUserId: String,
        orderId: String,
    ): OrderResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
            if (order.shopId?.value != seller.shopId?.value) throw ForbiddenException("Order belongs to another shop")
            if (order.fulfillmentType != FulfillmentType.PICKUP) throw ValidationException("Order is not a pickup order")
            if (order.paymentStatus != PaymentStatus.COMPLETED && order.paymentMethod != PaymentMethod.COD) {
                throw ValidationException("Order is not paid")
            }
            if (order.status == OrderStatus.CANCELED) throw ValidationException("Order is canceled")
            if (order.pickupStatus != null && order.pickupStatus != PickupStatus.PENDING) {
                throw ValidationException("Pickup order cannot be marked ready")
            }
            order.pickupStatus = PickupStatus.READY
            order.toOrderResponse(includeSensitive = false)
        }

    suspend fun completePickup(
        sellerUserId: String,
        orderId: String,
        code: String,
    ): OrderResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
            if (order.shopId?.value != seller.shopId?.value) throw ForbiddenException("Order belongs to another shop")
            if (order.fulfillmentType != FulfillmentType.PICKUP) throw ValidationException("Order is not a pickup order")
            val collectCashAtPickup =
                order.paymentMethod == PaymentMethod.COD && order.paymentStatus == PaymentStatus.PENDING
            if (order.paymentStatus != PaymentStatus.COMPLETED && !collectCashAtPickup) {
                throw ValidationException("Order is not paid")
            }
            if (order.status == OrderStatus.CANCELED) throw ValidationException("Order is canceled")
            if (order.pickupStatus != PickupStatus.READY) throw ValidationException("Pickup order is not ready")
            if (order.pickupCode == null || order.pickupCode != code.trim().uppercase()) {
                throw ValidationException("Invalid pickup code")
            }
            val fromStatus = order.status
            order.pickupStatus = PickupStatus.PICKED_UP
            order.pickedUpAt = currentUtc()
            order.status = OrderStatus.RECEIVED
            order.completedDate = currentUtc()
            if (collectCashAtPickup) {
                val payment =
                    PaymentDAO.find {
                        (PaymentTable.orderId eq order.id) and (PaymentTable.status eq PaymentStatus.PENDING)
                    }.firstOrNull()
                payment?.apply {
                    status = PaymentStatus.COMPLETED
                    providerStatus = "collected-at-pickup"
                }
                order.paymentStatus = PaymentStatus.COMPLETED
                loyaltyService.earnForPaidOrderTx(order.userId.value, order.id.value, order.total)
                digitalDelivery.grantForOrderTx(order)
                payment?.let {
                    OutboxPublisher.enqueueTx(
                        OutboxAggregate.PAYMENT,
                        it.id.value,
                        PaymentCompletedEvent(
                            paymentId = it.id.value,
                            orderId = order.id.value,
                            userId = order.userId.value,
                            email = UserDAO.findById(order.userId.value)?.email.orEmpty(),
                            amount = it.amount,
                        ),
                    )
                }
            }
            OrderStatusHistoryDAO.new {
                this.orderId = order.id
                this.status = OrderStatus.RECEIVED
                notes = "Pickup completed"
                changedBy = sellerUserId.entityID(UserTable)
            }
            OrderEvents.statusChangedTx(order, fromStatus, OrderStatus.RECEIVED)
            order.toOrderResponse(includeSensitive = false)
        }

    suspend fun posSale(
        sellerUserId: String,
        request: PosSaleRequest,
    ): OrderResponse = pos.posSale(sellerUserId, request)

    suspend fun openPosSession(
        sellerUserId: String,
        request: PosSessionOpenRequest,
    ): PosSessionResponse = pos.openPosSession(sellerUserId, request)

    suspend fun closePosSession(
        sellerUserId: String,
        sessionId: String,
        request: PosSessionCloseRequest,
    ): PosSessionResponse = pos.closePosSession(sellerUserId, sessionId, request)

    suspend fun posReceipt(
        sellerUserId: String,
        orderId: String,
    ): String = pos.posReceipt(sellerUserId, orderId)

    suspend fun posSessionReport(
        sellerUserId: String,
        sessionId: String,
    ): String = pos.posSessionReport(sellerUserId, sessionId)

    suspend fun posSessions(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PosSessionResponse> = pos.posSessions(sellerUserId, limit, offset)

    suspend fun posSales(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> = pos.posSales(sellerUserId, limit, offset)

    private fun WarehouseDAO.toStoreResponse(
        latitude: Double?,
        longitude: Double?,
    ) = StoreResponse(
        id = id.value,
        shopId = shopId.value,
        name = name,
        address = address,
        city = city,
        country = country,
        latitude = this.latitude?.toPlainString(),
        longitude = this.longitude?.toPlainString(),
        phone = phone,
        openingHours = openingHours,
        supportsPickup = supportsPickup,
        supportsPos = supportsPos,
        distanceKm =
            run {
                val storeLat = this.latitude
                val storeLon = this.longitude
                if (latitude != null && longitude != null && storeLat != null && storeLon != null) {
                    haversineKm(latitude, longitude, storeLat.toDouble(), storeLon.toDouble())
                } else {
                    null
                }
            },
    )

    private fun haversineKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val radius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return radius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
