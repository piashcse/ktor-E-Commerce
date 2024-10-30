package com.piashcse.route


import com.piashcse.controller.OrderController
import com.piashcse.models.order.AddOrder
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.OrderStatus
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
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
                    queryParameter<String>("offset") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("limit", "offset")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (limit, offset) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        orderController.getOrders(
                            call.currentUser().userId, limit.toInt(), offset.toLong()
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
                val requiredParams = listOf("id")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (id) = requiredParams.map { call.parameters[it]!! }
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
                val requiredParams = listOf("id")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (id) = requiredParams.map { call.parameters[it]!! }
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
                val requiredParams = listOf("id")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (id) = requiredParams.map { call.parameters[it]!! }
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
                val requiredParams = listOf("id")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (id) = requiredParams.map { call.parameters[it]!! }
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
