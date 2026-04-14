package com.piashcse.feature.order

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.UserType
import com.piashcse.model.request.CancelOrderRequest
import com.piashcse.model.request.OrderRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.InvalidEnumValueException
import com.piashcse.utils.UnauthorizedException
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.getCurrentUserType
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines all order-related routes for customers, sellers, and admins.
 *
 * - Customers can create orders and update their own orders to "CANCELED" or "RECEIVED".
 * - Sellers can update orders to "CONFIRMED" or "DELIVERED".
 * - Admins can update all order statuses.
 *
 * @param orderController The service that handles order operations.
 */
fun Route.orderRoutes(orderController: OrderService) {
    route("/order") {

        authenticate(RoleManagement.CUSTOMER.role) {
            /**
             * @tag Order
             * @description Create a new order with the provided order details
             * @operationId createOrder
             * @body OrderRequest Order creation request with order items and shipping info
             * @header Idempotency-Key Optional key to prevent duplicate orders
             * @response 200 Order created successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<OrderRequest>()
                val idempotencyKey = call.request.headers["Idempotency-Key"]
                call.respond(HttpStatusCode.OK, orderController.createOrder(call.currentUserId, requestBody, idempotencyKey))
            }

            /**
             * @tag Order
             * @description Retrieve all orders for the authenticated customer
             * @operationId getOrders
             * @query limit (required) Maximum number of orders to return
             * @response 200 Orders retrieved successfully
             * @security jwtToken
             */
            get {
                val (limit) = call.requireParameters("limit")
                call.respond(HttpStatusCode.OK, orderController.getOrders(call.currentUserId, limit.toInt()))
            }
        }

        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role) {
            /**
             * @tag Order
             * @description Update the status of an order (customer: CANCELED/RECEIVED, seller: CONFIRMED/DELIVERED)
             * @operationId updateOrderStatus
             * @path id (required) Unique identifier of the order to update
             * @query status (required) New order status (CANCELED, RECEIVED, CONFIRMED, or DELIVERED)
             * @response 200 Order status updated successfully
             * @security jwtToken
             */
            patch("status/{id}") {
                val (id, statusParam) = call.requireParameters("id", "status")
                val userId = call.currentUserId

                val status = try {
                    OrderStatus.valueOf(statusParam.uppercase())
                } catch (e: IllegalArgumentException) {
                    throw InvalidEnumValueException(
                        message = "Invalid order status: $statusParam",
                        enumName = OrderStatus.values().joinToString(", ") { it.name },
                        invalidValue = statusParam
                    )
                }

                val isSeller =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() == RoleManagement.SELLER.role
                val isCustomer = call.principal<JWTPrincipal>()?.payload?.getClaim("role")
                    ?.asString() == RoleManagement.CUSTOMER.role

                val sellerOnlyStatuses = listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED)
                val customerOnlyStatuses = listOf(OrderStatus.CANCELED, OrderStatus.RECEIVED)

                if ((status in sellerOnlyStatuses && !isSeller) || (status in customerOnlyStatuses && !isCustomer)) {
                    throw UnauthorizedException(Message.Orders.STATUS_NOT_ALLOWED)
                }

                call.respond(HttpStatusCode.OK, orderController.updateOrderStatus(userId, id, status))
            }
        }

        authenticate(RoleManagement.ADMIN.role, RoleManagement.SUPER_ADMIN.role) {
            /**
             * @tag Order
             * @description Admin-only: Update the status of any order to any valid status
             * @operationId updateOrderStatusByAdmin
             * @path id (required) Unique identifier of the order to update
             * @query status (required) New order status value
             * @response 200 Order status updated successfully
             * @security jwtToken
             */
            patch("status/{id}") {
                val (id, statusParam) = call.requireParameters("id", "status")
                val userId = call.currentUserId

                val status = try {
                    OrderStatus.valueOf(statusParam.uppercase())
                } catch (e: IllegalArgumentException) {
                    throw InvalidEnumValueException(
                        message = "Invalid order status: $statusParam",
                        enumName = OrderStatus.values().joinToString(", ") { it.name },
                        invalidValue = statusParam
                    )
                }

                call.respond(HttpStatusCode.OK, orderController.updateOrderStatus(userId, id, status))
            }
        }

        // Cancel order - accessible by customer, seller, admin
        authenticate(
            RoleManagement.CUSTOMER.role,
            RoleManagement.SELLER.role,
            RoleManagement.ADMIN.role,
            RoleManagement.SUPER_ADMIN.role
        ) {
            /**
             * @tag Order
             * @description Cancel an order (only PENDING or CONFIRMED orders can be canceled)
             * @operationId cancelOrder
             * @path id (required) Unique identifier of the order to cancel
             * @body CancelOrderRequest Cancellation request with reason
             * @response 200 Order canceled successfully
             * @response 400 Order cannot be canceled
             * @security jwtToken
             */
            post("{id}/cancel") {
                val id = call.parameters["id"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Order ID is required")

                val userId = call.currentUserId
                val userType = call.getCurrentUserType()
                    ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED)

                val requestBody = call.receive<CancelOrderRequest>()
                requestBody.validation()

                call.respond(
                    HttpStatusCode.OK,
                    orderController.cancelOrder(id, userId, requestBody.reason, userType)
                )
            }
        }

        // Seller order listing
        authenticate(RoleManagement.SELLER.role) {
            /**
             * @tag Order
             * @description Retrieve orders for the seller's shop
             * @operationId getSellerOrders
             * @query limit (required) Maximum number of orders to return
             * @query offset (optional) Offset for pagination
             * @query status (optional) Filter by order status
             * @response 200 Orders retrieved successfully
             * @security jwtToken
             */
            get("seller") {
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0
                val status = call.parameters["status"]

                call.respond(
                    HttpStatusCode.OK,
                    orderController.getSellerOrders(call.currentUserId, limit, offset, status)
                )
            }
        }

        // Admin order listing with filters
        authenticate(RoleManagement.ADMIN.role, RoleManagement.SUPER_ADMIN.role) {
            /**
             * @tag Order
             * @description Admin-only: Retrieve all orders with optional filters
             * @operationId getAdminOrders
             * @query limit (required) Maximum number of orders to return
             * @query offset (optional) Offset for pagination
             * @query status (optional) Filter by order status
             * @query startDate (optional) Filter by start date (ISO format)
             * @query endDate (optional) Filter by end date (ISO format)
             * @response 200 Orders retrieved successfully with total count
             * @security jwtToken
             */
            get("admin") {
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0
                val status = call.parameters["status"]
                val startDate = call.parameters["startDate"]?.let {
                    try {
                        java.time.Instant.parse(it)
                    } catch (e: Exception) { null }
                }
                val endDate = call.parameters["endDate"]?.let {
                    try {
                        java.time.Instant.parse(it)
                    } catch (e: Exception) { null }
                }

                val (orders, totalCount) = orderController.getAdminOrders(limit, offset, status, startDate, endDate)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "orders" to orders,
                        "total" to totalCount,
                        "page" to (offset / limit) + 1,
                        "limit" to limit
                    )
                )
            }
        }
    }
}
