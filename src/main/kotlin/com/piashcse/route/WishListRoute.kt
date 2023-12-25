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
        authenticateWithJwt(RoleManagement.CUSTOMER.role) {
            post<ProductId, Response, Unit, JwtTokenBody> { params, _ ->
                params.validation()
                respond(
                    ApiResponse.success(
                        wishlistController.addToWishList(principal().userId, params.productId), HttpStatusCode.OK
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