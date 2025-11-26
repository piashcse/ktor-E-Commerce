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
    route("wishlist") {
        authenticate(RoleManagement.CUSTOMER.role) {

            /**
             * POST request to add a product to the user's wish list.
             *
             * @tag Wish List
             * @summary auth[customer]
             * @body [WishListRequest] The product to add to the wish list.
             * @response 200 [ApiResponse] Success response after adding to wishlist
             */
            post("/wishlist") {
                val requestBody = call.receive<WishListRequest>()
                call.respond(
                    ApiResponse.success(
                        wishlistController.addToWishList(call.currentUser().userId, requestBody.productId),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * GET request to retrieve the user's wish list.
             *
             * @tag Wish List
             * @summary auth[customer]
             * @query limit The maximum number of products to retrieve from the wish list.
             * @response 200 [ApiResponse] Success response with wishlist items
             * @response 400 Bad request if limit is missing
             */
            get("/wishlist") {
                val (limit) = call.requiredParameters("limit") ?: return@get
                call.respond(
                    ApiResponse.success(
                        wishlistController.getWishList(call.currentUser().userId, limit.toInt()),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * DELETE request to remove a product from the user's wish list.
             *
             * @tag Wish List
             * @summary auth[customer]
             * @query productId The ID of the product to remove from the wish list.
             * @response 200 [ApiResponse] Success response after removing from wishlist
             * @response 400 Bad request if productId is missing
             */
            delete("/wishlist/remove") {
                val (productId) = call.requiredParameters("productId") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        wishlistController.removeFromWishList(call.currentUser().userId, productId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}