package com.piashcse.feature.order
import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.UserType
import com.piashcse.model.request.CancelOrderRequest
import com.piashcse.plugin.customerAuth
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.UnauthorizedException
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.time.Instant

/**
 * Order-related routes for customers and sellers.
 */
fun Route.orderRoutes() {
    val orderRepo: OrderRepository by inject()
    customerAuth {
        /**
         * @tag Order
         * @description Retrieve all orders for the authenticated customer
         */
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(orderRepo.getOrders(call.currentUserId, limit, offset))
        }
    }

    requireRole(UserType.CUSTOMER, UserType.SELLER) {
        /**
         * @tag Order
         * @description Update order status (Customer: CANCELED/RECEIVED, Seller: CONFIRMED/DELIVERED)
         */
        patch("status/{id}") {
            val id = call.requirePathParameter("id")
            val status = call.requireQueryParameter("status").parseEnum<OrderStatus>("status")
            val userType = call.getCurrentUserType() ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED)

            if ((status in listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED) && userType != UserType.SELLER) ||
                (status in listOf(OrderStatus.CANCELED, OrderStatus.RECEIVED) && userType != UserType.CUSTOMER)
            ) throw UnauthorizedException(Message.Orders.STATUS_NOT_ALLOWED)

            call.respondOk(orderRepo.updateOrderStatus(call.currentUserId, id, status))
        }

        /**
         * @tag Order
         * @description Cancel an order
         */
        post("{id}/cancel") {
            call.respondOk(orderRepo.cancelOrder(call.requirePathParameter("id"), call.currentUserId, call.receive<CancelOrderRequest>().reason, call.getCurrentUserType() ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED)))
        }
    }
}

/**
 * Seller order management routes.
 */
fun Route.orderSellerRoutes() {
    val orderRepo: OrderRepository by inject()
    /**
     * @tag Order
     * @description Seller: Retrieve orders for the seller's shop
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        val status = call.request.queryParameters["status"]
        call.respondOk(orderRepo.getSellerOrders(call.currentUserId, limit, offset, status))
    }
}

/**
 * Admin order management routes.
 */
fun Route.orderAdminRoutes() {
    val orderRepo: OrderRepository by inject()
    /**
     * @tag Order
     * @description Admin: Update the status of any order
     */
    patch("status/{id}") {
        val id = call.requirePathParameter("id")
        val status = call.requireQueryParameter("status").parseEnum<OrderStatus>("status")
        call.respondOk(orderRepo.updateOrderStatus(call.currentUserId, id, status))
    }

    /**
     * @tag Order
     * @description Admin: Cancel any order
     */
        post("{id}/cancel") {
            call.respondOk(orderRepo.cancelOrder(call.requirePathParameter("id"), call.currentUserId, call.receive<CancelOrderRequest>().reason, UserType.ADMIN))
        }

    /**
     * @tag Order
     * @description Admin: Retrieve all orders with advanced filtering
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        val status = call.request.queryParameters["status"]
        val startDate =
            call.request.queryParameters["startDate"]?.let {
                runCatching { Instant.parse(it) }.getOrNull()
            }
        val endDate =
            call.request.queryParameters["endDate"]?.let {
                runCatching { Instant.parse(it) }.getOrNull()
            }

        call.respondOk(orderRepo.getAdminOrders(limit, offset, status, startDate, endDate))
    }
}
