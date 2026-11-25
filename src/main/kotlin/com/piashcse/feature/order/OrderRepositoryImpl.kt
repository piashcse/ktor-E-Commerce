package com.piashcse.feature.order

import com.piashcse.constants.*
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.mapper.toOrderResponse
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.service.CatalogCache
import com.piashcse.utils.common.CursorPage
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.*
import java.time.Instant
import java.util.*

class OrderRepositoryImpl(
    private val placement: OrderPlacementService,
) : OrderRepository {
    override suspend fun placeOrder(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): List<OrderResponse> = placement.placeOrder(userId, checkoutRequest)

    override suspend fun getCheckoutSummary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutSummaryResponse = placement.checkoutSummary(userId, checkoutRequest)

    override suspend fun getOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
        query: String?,
        from: Instant?,
        to: Instant?,
    ): PaginatedResponse<OrderResponse> = OrderQueries.getOrders(userId, limit, offset, status, query, from, to)

    override suspend fun getOrdersCursor(
        userId: String,
        limit: Int,
        cursor: String?,
    ): CursorPage<OrderResponse> = OrderQueries.getOrdersCursor(userId, limit, cursor)

    override suspend fun getOrderForGuest(
        orderNumber: String,
        email: String,
    ): OrderResponse? = OrderQueries.getOrderForGuest(orderNumber, email)

    override suspend fun getOrderById(
        orderId: String,
        userId: String,
        userType: UserType,
    ): OrderResponse = OrderQueries.getOrderById(orderId, userId, userType)

    override suspend fun updateOrderStatus(
        userId: String,
        orderId: String,
        status: OrderStatus,
    ): OrderResponse {
        val result =
            query {
                userId.requireNotBlank("User ID")
                orderId.requireNotBlank("Order ID")

                val order =
                    OrderDAO.find { OrderTable.id eq orderId.entityID(OrderTable) }
                        .forUpdate()
                        .firstOrNull()
                        ?: throw ValidationException(Message.Orders.NOT_FOUND)
                val user = UserDAO.findById(userId) ?: throw ValidationException(Message.Errors.NOT_FOUND)

                requireOrderAccess(order.userId.value, order.shopId?.value, userId, user.userType)

                val offlineShipment =
                    status == OrderStatus.SHIPPED &&
                        order.status == OrderStatus.CONFIRMED &&
                        order.paymentMethod in listOf(PaymentMethod.COD, PaymentMethod.BANK_TRANSFER)
                if (!OrderStatus.canTransitionTo(order.status, status) && !offlineShipment) {
                    throw ValidationException(Message.Orders.INVALID_STATUS)
                }
                if (status == OrderStatus.PAID) {
                    // PAID is owned by the payment pipeline so payout, loyalty,
                    // digital delivery and reservation finalization cannot be skipped.
                    throw ValidationException(
                        "Order cannot be marked paid directly; confirm the payment instead",
                    )
                }

                val fromStatus = order.status
                if (status == OrderStatus.CANCELED) {
                    OrderCancellation.cancel(order, notes = "Status updated by user", changedBy = userId)
                } else {
                    order.status = status
                    when (status) {
                        OrderStatus.SHIPPED -> {
                            order.shippingDate = currentUtc()
                        }
                        OrderStatus.DELIVERED -> {
                            order.deliveredDate = currentUtc()
                            if (order.shippingDate == null) order.shippingDate = order.deliveredDate
                        }
                        OrderStatus.RECEIVED -> order.completedDate = currentUtc()
                        OrderStatus.PAID -> order.paymentStatus = PaymentStatus.COMPLETED
                        else -> {}
                    }
                    OrderStatusHistory.log(order.id.value, status, "Status updated by user", userId)
                }
                if (status in setOf(OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.RECEIVED, OrderStatus.CANCELED)) {
                    OrderEvents.statusChangedTx(order, fromStatus, status, order.trackingNumber)
                }
                order.toOrderResponse(
                    OrderItemDAO.itemsForOrder(order.id).toItemResponses(),
                    includeSensitive = user.userType == UserType.CUSTOMER,
                    forCustomer = user.userType == UserType.CUSTOMER,
                )
            }
        CatalogCache.invalidate()
        return result
    }

    override suspend fun cancelOrder(
        orderId: String,
        userId: String,
        reason: String,
        userType: UserType,
    ): OrderResponse {
        val result =
            retryQuery {
                orderId.requireNotBlank("Order ID")
                if (reason.isBlank()) throw ValidationException(Message.Orders.CANCEL_REASON_REQUIRED)

                val order =
                    OrderDAO.find { OrderTable.id eq orderId.entityID(OrderTable) }
                        .forUpdate()
                        .firstOrNull()
                        ?: throw ValidationException(Message.Orders.NOT_FOUND)

                requireOrderAccess(order.userId.value, order.shopId?.value, userId, userType)
                if (!OrderStatus.canBeCanceled(order.status)) throw ValidationException(Message.Orders.CANNOT_CANCEL)

                val fromStatus = order.status
                OrderCancellation.cancel(order, notes = reason, changedBy = userId)
                OrderEvents.statusChangedTx(order, fromStatus, OrderStatus.CANCELED)
                order.toOrderResponse(
                    OrderItemDAO.itemsForOrder(order.id).toItemResponses(),
                    includeSensitive = userType == UserType.CUSTOMER,
                    forCustomer = userType == UserType.CUSTOMER,
                )
            }
        CatalogCache.invalidate()
        return result
    }

    override suspend fun shipOrder(
        orderId: String,
        userId: String,
        trackingNumber: String,
    ): OrderResponse {
        trackingNumber.requireNotBlank("Tracking number")
        val result =
            query {
                val order =
                    OrderDAO.find { OrderTable.id eq orderId.entityID(OrderTable) }
                        .forUpdate()
                        .firstOrNull()
                        ?: throw ValidationException(Message.Orders.NOT_FOUND)
                val user = UserDAO.findById(userId) ?: throw ValidationException(Message.Errors.NOT_FOUND)
                requireOrderAccess(order.userId.value, order.shopId?.value, userId, user.userType)
                val offlineShipment =
                    order.status == OrderStatus.CONFIRMED &&
                        order.paymentMethod in listOf(PaymentMethod.COD, PaymentMethod.BANK_TRANSFER)
                if (!OrderStatus.canTransitionTo(order.status, OrderStatus.SHIPPED) && !offlineShipment) {
                    throw ValidationException(Message.Orders.INVALID_STATUS)
                }
                val fromStatus = order.status
                order.status = OrderStatus.SHIPPED
                order.shippingDate = currentUtc()
                order.trackingNumber = trackingNumber
                OrderStatusHistory.log(order.id.value, OrderStatus.SHIPPED, "Shipped via $trackingNumber", userId)
                OrderEvents.statusChangedTx(order, fromStatus, OrderStatus.SHIPPED, trackingNumber)
                order.toOrderResponse(
                    OrderItemDAO.itemsForOrder(order.id).toItemResponses(),
                    includeSensitive = false,
                )
            }
        CatalogCache.invalidate()
        return result
    }

    override suspend fun getSellerOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
        query: String?,
        from: Instant?,
        to: Instant?,
    ): PaginatedResponse<OrderResponse> = OrderQueries.getSellerOrders(userId, limit, offset, status, query, from, to)

    override suspend fun getAdminOrders(
        limit: Int,
        offset: Int,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
        query: String?,
    ): PaginatedResponse<OrderResponse> = OrderQueries.getAdminOrders(limit, offset, status, startDate, endDate, query)
}
