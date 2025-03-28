package com.piashcse.route

import com.piashcse.controller.CartController
import com.piashcse.models.cart.AddCart
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
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
                }
                apiResponse()
            }) {
                val (limit, offset) = call.requiredParameters("limit") ?: return@get
                call.respond(
                    ApiResponse.success(
                        cartController.getCartItems(
                            call.currentUser().userId,
                            limit.toInt()
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
                val (productId, quantity) = call.requiredParameters("productId", "quantity") ?: return@put
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
                val (productId) = call.requiredParameters("productId") ?: return@delete
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