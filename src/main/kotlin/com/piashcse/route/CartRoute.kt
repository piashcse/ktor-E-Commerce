package com.piashcse.route

import com.piashcse.controller.CartController
import com.piashcse.models.cart.AddCart
import com.piashcse.models.user.body.JwtTokenBody
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.getCurrentUser
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.cartRoute(cartController: CartController) {
    route("cart") {
        authenticate(RoleManagement.CUSTOMER.role) {
            post("", {
                tags("Cart")
                request {
                    body<AddCart>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddCart>()
                requestBody.validation()
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
            get("", {
                tags("Cart")
                request {
                    queryParameter<String>("limit")
                    queryParameter<String>("offset")
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
            delete("{productId}", {
                tags("Cart")
                request {
                    pathParameter<String>("productId")
                }
                apiResponse()
            }) {
                val requiredParams = listOf("productId")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (productId) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        cartController.removeCartItem(getCurrentUser().userId, productId),
                        HttpStatusCode.OK
                    )
                )
            }
            put("{productId}", {
                tags("Cart")
                request {
                    pathParameter<String>("productId")
                    queryParameter<String>("quantity")
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
            delete("all", {
                tags("Cart")
                apiResponse()
            }) {
                call.respond(
                    ApiResponse.success(
                        cartController.deleteAllFromCart(getCurrentUser().userId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}
/*
fun NormalOpenAPIRoute.cartRoute(cartController: CartController) {
    route("cart") {
        authenticateWithJwt(RoleManagement.CUSTOMER.role) {
            post<AddCart, Response, Unit, JwtTokenBody> { params, _ ->
                params.validation()
                respond(ApiResponse.success(cartController.addToCart(principal().userId, params), HttpStatusCode.OK))
            }
            route("/{productId}").delete<DeleteProduct, Response, JwtTokenBody> { params ->
                params.validation()
                respond(
                    ApiResponse.success(
                        cartController.removeCartItem(principal().userId, params), HttpStatusCode.OK
                    )
                )
            }
            route("/{productId}").put<UpdateCart, Response, Unit, JwtTokenBody> { params, _ ->
                params.validation()
                respond(
                    ApiResponse.success(
                        cartController.updateCartQuantity(principal().userId, params), HttpStatusCode.OK
                    )
                )
            }
            get<PagingData, Response, JwtTokenBody> { pagingData ->
                pagingData.validation()
                respond(
                    ApiResponse.success(
                        cartController.getCartItems(principal().userId, pagingData),
                        HttpStatusCode.OK
                    )
                )
            }
            route("/all").delete<Unit, Response, JwtTokenBody> { _ ->
                respond(
                    ApiResponse.success(
                        cartController.deleteAllFromCart(principal().userId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}*/
