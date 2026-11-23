package com.piashcse.feature.shipping

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.ShipmentStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemTable
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.ShipmentDAO
import com.piashcse.database.entities.ShipmentItemDAO
import com.piashcse.database.entities.ShipmentTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.order.OrderEvents
import com.piashcse.utils.db.isUniqueViolation
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireOrderAccess
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

/** Carrier label purchase, idempotency and shipment/order transitions. */
internal class ShipmentLabelService(
    private val carrier: CarrierGateway,
) {
    suspend fun purchaseLabel(
        orderId: String,
        userId: String,
        request: PurchaseShipmentRequest,
    ): ShipmentResponse {
        if (request.rateId.isBlank()) throw ValidationException("rateId is required")
        val carrierProvider = carrier.provider
        request.idempotencyKey?.takeIf { it.isNotBlank() }?.let { key ->
            val replay =
                query {
                    val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
                    val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
                    if (user.userType !in listOf(UserType.SELLER, UserType.ADMIN, UserType.SUPER_ADMIN)) {
                        throw ForbiddenException("Only sellers and admins can purchase shipping labels")
                    }
                    requireOrderAccess(order.userId.value, order.shopId?.value, userId, user.userType)
                    ShipmentDAO.find {
                        (ShipmentTable.orderId eq order.id) and (ShipmentTable.idempotencyKey eq key)
                    }.firstOrNull()
                }
            if (replay != null && replay.status != ShipmentStatus.FAILED) {
                return query { replay.toShipmentResponse() }
            }
            if (replay != null && replay.status == ShipmentStatus.FAILED) {
                // Retry the carrier with the same key: remove the failed row so
                // the unique (order_id, idempotency_key) index does not return
                // the old failure as a 201 success.
                query { replay.delete() }
            }
        }

        // Reserve the allocation before calling the carrier. A PENDING shipment
        // counts against shipped quantities, so concurrent purchases cannot
        // allocate the same units; a carrier failure marks it FAILED and frees them.
        val (shipment, allocation) =
            try {
                query {
                    val finalAllocation = ShipmentAllocation.planTx(orderId, userId, request.items)
                    val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
                    val reservation =
                        ShipmentDAO.new {
                            this.orderId = order.id
                            this.carrier = carrierProvider
                            this.rateId = request.rateId
                            this.idempotencyKey = request.idempotencyKey?.takeIf { it.isNotBlank() }
                            this.status = ShipmentStatus.PENDING
                            this.currency = order.currency
                        }
                    finalAllocation.forEach { line ->
                        ShipmentItemDAO.new {
                            this.shipmentId = reservation.id
                            this.orderItemId = line.orderItemId.entityID(OrderItemTable)
                            this.quantity = line.quantity
                        }
                    }
                    reservation to finalAllocation
                }
            } catch (error: Exception) {
                if (error.isUniqueViolation() && request.idempotencyKey != null) {
                    query {
                        ShipmentDAO.find {
                            (ShipmentTable.orderId eq orderId.entityID(OrderTable)) and
                                (ShipmentTable.idempotencyKey eq request.idempotencyKey)
                        }.firstOrNull()
                    }?.let { return it.toShipmentResponse() }
                }
                throw error
            }

        val label =
            try {
                carrier.purchaseLabel(CarrierLabelRequest(request.rateId, orderId))
            } catch (error: Exception) {
                query { shipment.status = ShipmentStatus.FAILED }
                throw error
            }
        if (!label.success) {
            query { shipment.status = ShipmentStatus.FAILED }
            throw ValidationException(label.message ?: "Carrier label purchase failed")
        }

        return query {
            val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
            val now = currentUtc()
            shipment.carrier = label.carrier ?: carrier.provider
            shipment.service = label.service
            shipment.trackingNumber = label.trackingNumber
            shipment.labelUrl = label.labelUrl
            shipment.providerReference = label.providerReference
            shipment.rateAmount = label.amount
            shipment.currency = label.currency
            shipment.status = ShipmentStatus.LABEL_PURCHASED
            shipment.shippedAt = now
            if (ShipmentAllocation.allUnitsShipped(order)) {
                if (order.status == OrderStatus.PAID) {
                    val from = order.status
                    order.status = OrderStatus.SHIPPED
                    order.shippingDate = now
                    label.trackingNumber?.let { order.trackingNumber = it }
                    OrderEvents.statusChangedTx(order, from, OrderStatus.SHIPPED, order.trackingNumber)
                } else {
                    label.trackingNumber?.let { order.trackingNumber = it }
                }
            }
            shipment.toShipmentResponse(allocation)
        }
    }
}
