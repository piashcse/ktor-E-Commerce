package com.piashcse.route

import com.papsign.ktor.openapigen.route.path.auth.*
import com.piashcse.controller.CartController
import com.piashcse.models.PagingData
import com.piashcse.models.user.body.JwtTokenBody
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.Response
import com.piashcse.utils.authenticateWithJwt
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.piashcse.models.cart.*
import io.ktor.http.*

fun NormalOpenAPIRoute.cartRoute(cartController: CartController) {
    route("cart") {
        authenticateWithJwt(RoleManagement.USER.role) {
            route("/add").post<Unit, Response, AddCart, JwtTokenBody>(
                exampleRequest = AddCart(
                    "",
                    1,
                )
            ) { _, requestBody ->
                requestBody.validation()
                respond(ApiResponse.success(cartController.addToCart(principal().userId, requestBody), HttpStatusCode.OK))
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
            route("/all").delete<DeleteProduct, Response, JwtTokenBody> { params ->
                params.validation()
                respond(
                    ApiResponse.success(
                        cartController.deleteAllFromCart(principal().userId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}