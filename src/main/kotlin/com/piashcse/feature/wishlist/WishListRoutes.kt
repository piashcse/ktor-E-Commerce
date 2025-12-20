package com.piashcse.feature.wishlist

import com.piashcse.model.request.WishListRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Route for managing the user's wish list operations.
 *
 * @param wishlistController The controller responsible for handling wish list operations.
 */
fun Route.wishListRoutes(wishlistController: WishListService) {
    route("/wishlist") {
        authenticate(RoleManagement.CUSTOMER.role) {

            /**
             * @tag WishList
             * @body [WishListRequest]
             * @response 200 [Response]
             */
            post {
                val requestBody = call.receive<WishListRequest>()
                requestBody.validation()
                call.respond(
                    ApiResponse.success(
                        wishlistController.addToWishList(call.currentUser().userId, requestBody.productId),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag WishList
             * @query limit (optional, default 10)
             * @query page (optional, default 1)
             * @response 200 [Response]
             * @response 400
             */
            get {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val offset = ((page - 1) * limit).toLong()
                
                call.respond(
                    ApiResponse.success(
                        wishlistController.getWishList(call.currentUser().userId, limit, offset),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag WishList
             * @query productId (required)
             * @response 200 [Response]
             * @response 400
             */
            delete("remove") {
                val (productId) = call.requiredParameters("productId") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        wishlistController.removeFromWishList(call.currentUser().userId, productId), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag WishList
             * @query productId (required)
             * @response 200 [Response]
             */
            get("check") {
                val (productId) = call.requiredParameters("productId") ?: return@get
                call.respond(
                    ApiResponse.success(
                        wishlistController.isProductInWishList(call.currentUser().userId, productId),
                        HttpStatusCode.OK
                    )
                )
            }
        }
    }
}