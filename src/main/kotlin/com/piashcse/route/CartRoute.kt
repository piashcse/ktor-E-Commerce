package com.piashcse.route

import com.piashcse.controller.CartController
import com.piashcse.models.cart.AddCart
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.getCurrentUser
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.cartRoute(cartController: CartController) {
    route("cart") {
        authenticate(RoleManagement.CUSTOMER.role) {
            post({
                tags("Cart")
                request {
                    body<AddCart>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddCart>()
                call.respond(
                    ApiResponse.success(
                        cartController.addToCart(
                            getCurrentUser().userId,
                            requestBody.productId,
                            requestBody.quantity
                        ), HttpStatusCode.OK
                    )
                )
            }
            get({
                tags("Cart")
                request {
                    queryParameter<Int>("limit") {
                        required = true
                    }
                    queryParameter<Long>("offset") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("limit", "offset")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (limit, offset) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        cartController.getCartItems(
                            getCurrentUser().userId,
                            limit.toInt(),
                            offset.toLong()
                        ), HttpStatusCode.OK
                    )
                )
            }
            put({
                tags("Cart")
                request {
                    queryParameter<String>("productId") {
                        required = true
                    }
                    queryParameter<String>("quantity") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("productId", "quantity")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (productId, quantity) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        cartController.updateCartQuantity(getCurrentUser().userId, productId, quantity.toInt()),
                        HttpStatusCode.OK
                    )
                )
            }
            delete({
                tags("Cart")
                request {
                    queryParameter<String>("productId") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val productId = call.parameters["productId"]!!
                call.respond(
                    ApiResponse.success(
                        cartController.removeCartItem(getCurrentUser().userId, productId),
                        HttpStatusCode.OK
                    )
                )
            }
            delete("all", {
                tags("Cart")
                apiResponse()
            }) {
                call.respond(
                    ApiResponse.success(
                        cartController.deleteAllItemsOfCart(getCurrentUser().userId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}