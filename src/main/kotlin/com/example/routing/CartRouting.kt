package com.example.routing

import com.example.controller.CartController
import com.example.models.PagingData
import com.example.models.cart.AddCart
import com.example.models.user.body.JwtTokenBody
import com.example.plugins.RoleManagement
import com.example.utils.ApiResponse
import com.example.utils.Response
import com.example.utils.authenticateWithJwt
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.path.auth.post
import com.papsign.ktor.openapigen.route.path.auth.principal
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*

fun NormalOpenAPIRoute.cartRouting(cartController: CartController) {
    route("cart") {
        authenticateWithJwt(RoleManagement.USER.role) {
            post<Unit, Response, AddCart, JwtTokenBody>(
                exampleRequest = AddCart(
                    "",
                    100f,
                    100f,
                    2,
                )
            ) { _, cartBody ->
                cartBody.validation()
                respond(ApiResponse.success(cartController.createCart(principal().userId, cartBody), HttpStatusCode.OK))
            }
            get<PagingData, Response, JwtTokenBody> { pagingData ->
                pagingData.validation()
                respond(ApiResponse.success(cartController.getCartItems(pagingData), HttpStatusCode.OK))
            }
        }
    }
}