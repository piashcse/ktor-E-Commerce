package com.piashcse.route


import com.piashcse.controller.OrderController
import com.piashcse.models.order.AddOrder
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.OrderStatus
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

fun Route.orderRoute(orderController: OrderController) {
    route("/order") {
        authenticate(RoleManagement.CUSTOMER.role) {
            post({
                tags("Order")
                request {
                    body<AddOrder>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddOrder>()
                call.respond(
                    ApiResponse.success(
                        orderController.addOrder(call.currentUser().userId, requestBody), HttpStatusCode.OK
                    )
                )
            }
            get({
                tags("Order")
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
            put("{id}/cancel", {
                tags("Order")
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
                        orderController.updateOrder(call.currentUser().userId, id, OrderStatus.CANCELED),
                        HttpStatusCode.OK
                    )
                )
            }
            put("{id}/receive", {
                tags("Order")
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
                        orderController.updateOrder(call.currentUser().userId, id, OrderStatus.RECEIVED),
                        HttpStatusCode.OK
                    )
                )
            }
        }
        authenticate(RoleManagement.SELLER.role) {
            put("{id}/confirm", {
                tags("Order")
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
                        orderController.updateOrder(call.currentUser().userId, id, OrderStatus.CANCELED),
                        HttpStatusCode.OK
                    )
                )
            }
            put("{id}/deliver", {
                tags("Order")
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
                        orderController.updateOrder(call.currentUser().userId, id, OrderStatus.DELIVERED),
                        HttpStatusCode.OK
                    )
                )
            }
        }
    }
}
