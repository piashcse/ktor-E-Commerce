package com.piashcse.modules.order.routes


import com.piashcse.modules.order.controller.OrderController
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.models.order.OrderRequest
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines routes for managing orders.
 *
 * Accessible by customers and sellers with different roles and actions allowed for each.
 *
 * @param orderController The controller handling order-related operations.
 */
fun Route.orderRoutes(orderController: OrderController) {
    route("/order") {

        /**
         * POST request to create a new order.
         *
         * Accessible by customers only.
         *
         * @param orderRequest The order details (product ID, quantity, etc.) to create the order.
         */
        authenticate(RoleManagement.CUSTOMER.role) {
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
                        orderController.createOrder(call.currentUser().userId, requestBody), HttpStatusCode.OK
                    )
                )
            }

            /**
             * GET request to retrieve orders placed by the customer.
             *
             * Accessible by customers only.
             *
             * @param limit The maximum number of orders to retrieve.
             */
            get({
                tags("Order")
                summary = "auth[customer]"
                request {
                    queryParameter<String>("limit") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (limit) = call.requiredParameters("limit") ?: return@get
                call.respond(
                    ApiResponse.success(
                        orderController.getOrders(
                            call.currentUser().userId, limit.toInt()
                        ), HttpStatusCode.OK
                    )
                )
            }

            /**
             * PUT request to cancel an order.
             *
             * Accessible by customers only.
             *
             * @param id The order ID to cancel.
             */
            put("{id}/cancel", {
                tags("Order")
                summary = "auth[customer]"
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (id) = call.requiredParameters("id") ?: return@put
                call.respond(
                    ApiResponse.success(
                        orderController.updateOrderStatus(call.currentUser().userId, id, OrderTable.OrderStatus.CANCELED),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * PUT request to mark an order as received.
             *
             * Accessible by customers only.
             *
             * @param id The order ID to mark as received.
             */
            put("{id}/receive", {
                tags("Order")
                summary = "auth[customer]"
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (id) = call.requiredParameters("id") ?: return@put
                call.respond(
                    ApiResponse.success(
                        orderController.updateOrderStatus(call.currentUser().userId, id, OrderTable.OrderStatus.RECEIVED),
                        HttpStatusCode.OK
                    )
                )
            }
        }

        /**
         * Routes for sellers to confirm or deliver orders.
         */
        authenticate(RoleManagement.SELLER.role) {
            /**
             * PUT request to confirm an order.
             *
             * Accessible by sellers only.
             *
             * @param id The order ID to confirm.
             */
            put("{id}/confirm", {
                tags("Order")
                summary = "auth[seller]"
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (id) = call.requiredParameters("id") ?: return@put
                call.respond(
                    ApiResponse.success(
                        orderController.updateOrderStatus(call.currentUser().userId, id, OrderTable.OrderStatus.CONFIRMED),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * PUT request to mark an order as delivered.
             *
             * Accessible by sellers only.
             *
             * @param id The order ID to mark as delivered.
             */
            put("{id}/deliver", {
                tags("Order")
                summary = "auth[seller]"
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (id) = call.requiredParameters("id") ?: return@put
                call.respond(
                    ApiResponse.success(
                        orderController.updateOrderStatus(call.currentUser().userId, id, OrderTable.OrderStatus.DELIVERED),
                        HttpStatusCode.OK
                    )
                )
            }
        }
    }
}