package com.piashcse.feature.order
import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.UserType
import com.piashcse.model.request.CancelOrderRequest
import com.piashcse.plugin.customerAuth
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.getCurrentUserType
import com.piashcse.utils.extension.paginateQueryParams
import java.time.Instant
import com.piashcse.utils.validator.InvalidEnumValueException
import com.piashcse.utils.validator.UnauthorizedException
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun ApplicationCall.parseOrderStatusParam(): OrderStatus {
    val param = requireQueryParameter("status")
    return try {
        OrderStatus.valueOf(param.uppercase())
    } catch (_: IllegalArgumentException) {
        throw InvalidEnumValueException(
            message = "Invalid order status: $param",
            enumName = OrderStatus.values().joinToString(", ") { it.name },
            invalidValue = param,
        )
    }
}

/**
 * Order-related routes for customers and sellers.
 */
fun Route.orderRoutes(
    orderQueryService: OrderQueryService,
    orderManagementService: OrderManagementService,
) {
    customerAuth {
        /**
         * @tag Order
         * @description Retrieve all orders for the authenticated customer
         */
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respond(HttpStatusCode.OK, orderQueryService.getOrders(call.currentUserId, limit, offset))
        }
    }

    requireRole(UserType.CUSTOMER, UserType.SELLER) {
        /**
         * @tag Order
         * @description Update order status (Customer: CANCELED/RECEIVED, Seller: CONFIRMED/DELIVERED)
         */
        patch("status/{id}") {
            val id = call.requirePathParameter("id")
            val status = call.parseOrderStatusParam()
            val userId = call.currentUserId

            val isSeller = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() == UserType.SELLER.name.lowercase()
            val isCustomer = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() == UserType.CUSTOMER.name.lowercase()

            val sellerOnlyStatuses = listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED)
            val customerOnlyStatuses = listOf(OrderStatus.CANCELED, OrderStatus.RECEIVED)

            val isSellerStatusButNotSeller = status in sellerOnlyStatuses && !isSeller
            val isCustomerStatusButNotCustomer = status in customerOnlyStatuses && !isCustomer

            if (isSellerStatusButNotSeller || isCustomerStatusButNotCustomer) {
                throw UnauthorizedException(Message.Orders.STATUS_NOT_ALLOWED)
            }

            call.respond(HttpStatusCode.OK, orderManagementService.updateOrderStatus(userId, id, status))
        }

        /**
         * @tag Order
         * @description Cancel an order
         */
        post("{id}/cancel") {
            val id = call.requirePathParameter("id")
            val userId = call.currentUserId
            val userType = call.getCurrentUserType() ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED)
            val requestBody = call.receive<CancelOrderRequest>()

            call.respond(HttpStatusCode.OK, orderManagementService.cancelOrder(id, userId, requestBody.reason, userType))
        }
    }
}

/**
 * Seller order management routes.
 */
fun Route.orderSellerRoutes(orderQueryService: OrderQueryService) {
    /**
     * @tag Order
     * @description Seller: Retrieve orders for the seller's shop
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        val status = call.request.queryParameters["status"]
        call.respond(HttpStatusCode.OK, orderQueryService.getSellerOrders(call.currentUserId, limit, offset, status))
    }
}

/**
 * Admin order management routes.
 */
fun Route.orderAdminRoutes(
    orderQueryService: OrderQueryService,
    orderManagementService: OrderManagementService,
) {
    /**
     * @tag Order
     * @description Admin: Update the status of any order
     */
    patch("status/{id}") {
        val id = call.requirePathParameter("id")
        val status = call.parseOrderStatusParam()
        val userId = call.currentUserId
        call.respond(HttpStatusCode.OK, orderManagementService.updateOrderStatus(userId, id, status))
    }

    /**
     * @tag Order
     * @description Admin: Cancel any order
     */
    post("{id}/cancel") {
        val id = call.requirePathParameter("id")
        val userId = call.currentUserId
        val userType = UserType.ADMIN
        val requestBody = call.receive<CancelOrderRequest>()

        call.respond(HttpStatusCode.OK, orderManagementService.cancelOrder(id, userId, requestBody.reason, userType))
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

        call.respond(HttpStatusCode.OK, orderQueryService.getAdminOrders(limit, offset, status, startDate, endDate))
    }
}
