package com.piashcse.feature.omnichannel

import com.piashcse.constants.FulfillmentType
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.PickupStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemTable
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.database.entities.WarehouseTable
import com.piashcse.database.entities.addSales
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.database.entities.decrementStock
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.mapper.toOrderResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
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
) {
    init {
        if (items.isEmpty()) throw FieldValidationException(listOf(FieldViolation("items", "At least one item is required")))
    }
}

@Serializable
data class PickupCompleteRequest(val pickupCode: String)

class OmnichannelService(
    private val digitalDelivery: DigitalDeliveryService = DigitalDeliveryService(),
) {
    suspend fun stores(
        latitude: Double?,
        longitude: Double?,
        radiusKm: Double?,
        limit: Int,
    ): List<StoreResponse> =
        query {
            val rows =
                WarehouseDAO.find {
                    (WarehouseTable.isActive eq true) and (WarehouseTable.supportsPickup eq true)
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
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId?.value ?: throw ValidationException("Seller has no shop")
            val rows =
                OrderDAO.find {
                    (OrderTable.shopId eq shopId.entityID(com.piashcse.database.entities.ShopTable)) and
                        (OrderTable.fulfillmentType eq FulfillmentType.PICKUP)
                }.sortedByDescending { it.createdAt }
            val page = rows.drop(offset).take(limit).map { it.toOrderResponse() }
            PaginatedResponse(
                data = page,
                metadata = PaginationMetadata(totalCount = rows.size.toLong(), limit = limit, offset = offset),
            )
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
            if (order.pickupStatus != null && order.pickupStatus != PickupStatus.PENDING) {
                throw ValidationException("Pickup order cannot be marked ready")
            }
            order.pickupStatus = PickupStatus.READY
            order.toOrderResponse()
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
            if (order.pickupStatus != PickupStatus.READY) throw ValidationException("Pickup order is not ready")
            if (order.pickupCode == null || order.pickupCode != code.trim().uppercase()) {
                throw ValidationException("Invalid pickup code")
            }
            order.pickupStatus = PickupStatus.PICKED_UP
            order.pickedUpAt = currentUtc()
            order.status = OrderStatus.RECEIVED
            order.completedDate = currentUtc()
            order.toOrderResponse()
        }

    suspend fun posSale(
        sellerUserId: String,
        request: PosSaleRequest,
    ): OrderResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            request.warehouseId?.takeIf { it.isNotBlank() }?.let { warehouseId ->
                val warehouse =
                    WarehouseDAO.findById(warehouseId)
                        ?: throw ValidationException("Warehouse not found")
                if (warehouse.shopId.value != shopId.value) throw ForbiddenException("Warehouse belongs to another shop")
                if (!warehouse.supportsPos) throw ValidationException("Warehouse does not support POS")
            }
            val products =
                ProductDAO.find {
                    ProductTable.id.inList(request.items.map { it.productId.entityID(ProductTable) })
                }.associateBy { it.id.value }
            if (products.size != request.items.map { it.productId }.distinct().size) {
                throw ValidationException("One or more products were not found")
            }
            if (products.values.any { it.shopId?.value != shopId.value }) {
                throw ValidationException("All POS items must belong to the seller's shop")
            }

            val guestEmail = request.customerEmail?.takeIf { it.isNotBlank() } ?: "pos-${UUID.randomUUID()}@local"
            val customer =
                UserDAO.new {
                    email = guestEmail
                    userType = com.piashcse.constants.UserType.CUSTOMER
                    password = "!"
                    isVerified = true
                    isGuest = true
                }

            val orderNumber =
                "POS-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}-${UUID.randomUUID().toString().take(6).uppercase()}"
            var total = BigDecimal.ZERO
            val order =
                OrderDAO.new {
                    userId = customer.id
                    this.shopId = shopId
                    this.orderNumber = orderNumber
                    status = OrderStatus.PAID
                    paymentStatus = PaymentStatus.COMPLETED
                    subTotal = BigDecimal.ZERO
                    shippingCost = BigDecimal.ZERO
                    taxAmount = BigDecimal.ZERO
                    this.total = BigDecimal.ZERO
                    currency = "USD"
                    fxRate = BigDecimal.ONE
                    shippingMethod = "POS"
                    fulfillmentType = FulfillmentType.POS
                    paymentMethod = request.paymentMethod
                    notes = "In-store sale"
                }

            request.items.forEach { item ->
                val product = products.getValue(item.productId)
                val unit = Money.scale2(product.discountPrice ?: product.price)
                val lineTotal = Money.scale2(unit.multiply(BigDecimal(item.quantity)))
                product.decrementStock(item.quantity)
                product.addSales(item.quantity)
                total = total.add(lineTotal)
                OrderItemTable.insert {
                    it[orderId] = order.id
                    it[productId] = product.id
                    it[OrderItemTable.shopId] = seller.shopId!!
                    it[quantity] = item.quantity
                    it[price] = unit
                    it[OrderItemTable.total] = lineTotal
                    it[productName] = product.name
                    it[sku] = product.sku
                    it[isDigital] = product.isDigital
                    it[discountAmount] = BigDecimal.ZERO
                    it[taxAmount] = BigDecimal.ZERO
                    it[taxRate] = BigDecimal.ZERO
                    it[taxInclusive] = false
                }
            }
            order.subTotal = Money.scale2(total)
            order.total = Money.scale2(total)

            PaymentDAO.new {
                this.orderId = order.id
                this.userId = customer.id
                amount = order.total
                currency = "USD"
                status = PaymentStatus.COMPLETED
                paymentMethod = request.paymentMethod
                transactionId = "POS-${order.id.value}"
                provider = com.piashcse.constants.PaymentProvider.MANUAL
                providerStatus = "pos"
                scaStatus = com.piashcse.constants.ScaStatus.NOT_REQUIRED
            }
            digitalDelivery.grantForOrderTx(order)
            order.toOrderResponse()
        }

    suspend fun posSales(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId?.value ?: throw ValidationException("Seller has no shop")
            val rows =
                OrderDAO.find {
                    (OrderTable.shopId eq shopId.entityID(com.piashcse.database.entities.ShopTable)) and
                        (OrderTable.fulfillmentType eq FulfillmentType.POS)
                }.sortedByDescending { it.createdAt }
            PaginatedResponse(
                data = rows.drop(offset).take(limit).map { it.toOrderResponse() },
                metadata = PaginationMetadata(totalCount = rows.size.toLong(), limit = limit, offset = offset),
            )
        }

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
            if (latitude != null && longitude != null && this.latitude != null && this.longitude != null) {
                haversineKm(latitude, longitude, this.latitude!!.toDouble(), this.longitude!!.toDouble())
            } else {
                null
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
