package com.piashcse.route

import com.papsign.ktor.openapigen.route.path.auth.delete
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.path.auth.post
import com.papsign.ktor.openapigen.route.path.auth.principal
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.piashcse.controller.WishListController
import com.piashcse.models.product.request.ProductId
import com.piashcse.models.user.body.JwtTokenBody
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.Response
import com.piashcse.utils.authenticateWithJwt
import io.ktor.http.*

fun NormalOpenAPIRoute.wishListRoute(wishlistController: WishListController) {
    route("wishlist") {
        authenticateWithJwt(RoleManagement.USER.role) {
            post<Unit, Response, ProductId, JwtTokenBody>(
                exampleRequest = ProductId(
                    productId = "8eabd62f-fbb2-4fad-b440-3060f2e12dbc"
                )
            ) { _, requestBody ->
                requestBody.validation()
                respond(
                    ApiResponse.success(
                        wishlistController.addToWishList(principal().userId, requestBody.productId), HttpStatusCode.OK
                    )
                )
            }
            get<Unit, Response, JwtTokenBody> { _ ->
                respond(ApiResponse.success(wishlistController.getWishList(principal().userId), HttpStatusCode.OK))
            }
            delete<ProductId, Response, JwtTokenBody> { params ->
                params.validation()
                respond(
                    ApiResponse.success(
                        wishlistController.deleteFromWishList(principal().userId, params.productId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}