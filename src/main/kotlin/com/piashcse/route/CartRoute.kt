package com.piashcse.route

import com.piashcse.controller.CartController
import com.piashcse.models.cart.AddCart
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
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
                            call.currentUser().userId,
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
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (limit, offset) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        cartController.getCartItems(
                            call.currentUser().userId,
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
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (productId, quantity) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        cartController.updateCartQuantity(call.currentUser().userId, productId, quantity.toInt()),
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
                val requiredParams = listOf("productId")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (productId) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        cartController.deleteCartItem(call.currentUser().userId, productId),
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
                        cartController.deleteAllItemsOfCart(call.currentUser().userId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}