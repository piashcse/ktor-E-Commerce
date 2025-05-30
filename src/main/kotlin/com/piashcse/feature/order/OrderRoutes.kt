package com.piashcse.feature.order

import com.piashcse.constants.Message.INVALID_ORDER_STATUS
import com.piashcse.constants.Message.YOU_ARE_NOT_ALLOWED_TO_SET_STATUS
import com.piashcse.constants.OrderStatus
import com.piashcse.model.request.OrderRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.patch
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines all order-related routes for customers and sellers.
 *
 * - Customers can create orders and update their own orders to "CANCELED" or "RECEIVED".
 * - Sellers can update orders to "CONFIRMED" or "DELIVERED".
 *
 * @param orderController The service that handles order operations.
 */
fun Route.orderRoutes(orderController: OrderService) {
    route("/order") {

        authenticate(RoleManagement.CUSTOMER.role) {
            /**
             * POST request to create a new order.
             *
             * Accessible by customers only.
             *
             * @body OrderRequest The order data (product ID, quantity, etc.).
             */
            post({
                tags("Order")
                summary = "auth[customer]"
                request {
                    body<OrderRequest>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<OrderRequest>()
                call.respond(
                    ApiResponse.success(
                        orderController.createOrder(call.currentUser().userId, requestBody),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * GET request to fetch the list of orders by the current customer.
             *
             * Accessible by customers only.
             *
             * @queryParam limit The maximum number of orders to retrieve.
             */
            get({
                tags("Order")
                summary = "auth[customer]"
                request {
                    queryParameter<String>("limit") { required = true }
                }
                apiResponse()
            }) {
                val (limit) = call.requiredParameters("limit") ?: return@get
                call.respond(
                    ApiResponse.success(
                        orderController.getOrders(call.currentUser().userId, limit.toInt()),
                        HttpStatusCode.OK
                    )
                )
            }
        }

        /**
         * PATCH request to update order status.
         *
         * Accessible by both customers and sellers.
         * - Customers can update status to: CANCELED, RECEIVED
         * - Sellers can update status to: CONFIRMED, DELIVERED
         *
         * @queryParam id The ID of the order to update.
         * @queryParam status The new status to set (e.g., CONFIRMED, DELIVERED, CANCELED, RECEIVED).
         */
        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role) {
            patch("status/{id}", {
                tags("Order")
                summary = "auth[customer, seller]"
                request {
                    pathParameter<String>("id") { required = true }
                    queryParameter<String>("status") { required = true }
                }
                apiResponse()
            }) {
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
    }
}