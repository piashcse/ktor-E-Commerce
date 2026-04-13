package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.model.request.OrderRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.InvalidEnumValueException
import com.piashcse.utils.UnauthorizedException
import com.piashcse.utils.extension.currentUserId
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
             * @response 200 Order created successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<OrderRequest>()
                call.respond(ApiResponse.ok(orderController.createOrder(call.currentUserId, requestBody)))
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
                call.respond(ApiResponse.ok(orderController.getOrders(call.currentUserId, limit.toInt())))
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
                    throw UnauthorizedException("You are not allowed to set status to $status")
                }

                call.respond(ApiResponse.ok(orderController.updateOrderStatus(userId, id, status)))
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

                call.respond(ApiResponse.ok(orderController.updateOrderStatus(userId, id, status)))
            }
        }
    }
}