package com.piashcse.routing

import com.piashcse.controller.CartController
import com.piashcse.models.PagingData
import com.piashcse.models.cart.AddCart
import com.piashcse.models.user.body.JwtTokenBody
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.Response
import com.piashcse.utils.authenticateWithJwt
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