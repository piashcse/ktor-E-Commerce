package com.piashcse.feature.order

import com.piashcse.constants.Message.INVALID_ORDER_STATUS
import com.piashcse.constants.Message.YOU_ARE_NOT_ALLOWED_TO_SET_STATUS
import com.piashcse.constants.OrderStatus
import com.piashcse.model.request.OrderRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
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
             * @body [OrderRequest]
             * @response 200 [Response]
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<OrderRequest>()
                call.respond(
                    ApiResponse.success(
                        orderController.createOrder(call.currentUser().userId, requestBody),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Order
             * @query limit (required)
             * @response 200 [Response]
             * @security jwtToken
             */
            get {
                val (limit) = call.requiredParameters("limit") ?: return@get
                call.respond(
                    ApiResponse.success(
                        orderController.getOrders(call.currentUser().userId, limit.toInt()),
                        HttpStatusCode.OK
                    )
                )
            }
        }

        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role) {
            /**
             * @tag Order
             * @path id (required)
             * @query status (required)
             * @response 200 [Response]
             * @security jwtToken
             */
            patch("status/{id}") {
                val (id, statusParam) = call.requiredParameters("id", "status") ?: return@patch
                val user = call.currentUser()

                val status = try {
                    OrderStatus.valueOf(statusParam.uppercase())
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest, ApiResponse.failure(
                            "$INVALID_ORDER_STATUS $statusParam",
                            HttpStatusCode.BadRequest
                        )
                    )
                    return@patch
                }

                val isSeller =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() == RoleManagement.SELLER.role
                val isCustomer = call.principal<JWTPrincipal>()?.payload?.getClaim("role")
                    ?.asString() == RoleManagement.CUSTOMER.role

                val sellerOnlyStatuses = listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED)
                val customerOnlyStatuses = listOf(OrderStatus.CANCELED, OrderStatus.RECEIVED)

                if ((status in sellerOnlyStatuses && !isSeller) || (status in customerOnlyStatuses && !isCustomer)) {
                    call.respond(
                        HttpStatusCode.Unauthorized, ApiResponse.failure(
                            "$YOU_ARE_NOT_ALLOWED_TO_SET_STATUS $status",
                            HttpStatusCode.Unauthorized
                        )
                    )
                    return@patch
                }

                call.respond(
                    ApiResponse.success(
                        orderController.updateOrderStatus(user.userId, id, status),
                        HttpStatusCode.OK
                    )
                )
            }
        }

        // Admin and Super Admin routes for managing all orders
        authenticate(RoleManagement.ADMIN.role, RoleManagement.SUPER_ADMIN.role) {
            /**
             * @tag Order
             * @path id (required)
             * @query status (required)
             * @response 200 [Response]
             * @security jwtToken
             */
            patch("status/{id}") {
                val (id, statusParam) = call.requiredParameters("id", "status") ?: return@patch
                val user = call.currentUser()

                val status = try {
                    OrderStatus.valueOf(statusParam.uppercase())
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest, ApiResponse.failure(
                            "$INVALID_ORDER_STATUS $statusParam",
                            HttpStatusCode.BadRequest
                        )
                    )
                    return@patch
                }

                call.respond(
                    ApiResponse.success(
                        orderController.updateOrderStatus(user.userId, id, status),
                        HttpStatusCode.OK
                    )
                )
            }
        }
    }
}