package com.piashcse.feature.wishlist

import com.piashcse.model.request.WishListRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
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
    authenticate(RoleManagement.CUSTOMER.role) {

            /**
             * @tag WishList
             * @description Add a product to the user's wishlist
             * @operationId addToWishList
             * @body WishListRequest Wishlist request with product ID
             * @response 200 Product added to wishlist successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<WishListRequest>()
                requestBody.validation()
                call.respond(
                    HttpStatusCode.OK,
                    wishlistController.addToWishList(call.currentUserId, requestBody.productId)
                )
            }

            /**
             * @tag WishList
             * @description Retrieve all items in the user's wishlist with pagination
             * @operationId getWishList
             * @query limit Maximum number of items to return (default 20)
             * @query offset Number of items to skip (default 0)
             * @response 200 Wishlist items retrieved successfully
             * @response 400 Invalid pagination parameters
             * @security jwtToken
             */
            get {
                val (limit, offset) = call.paginationParameters(defaultLimit = 20)

                call.respond(
                    HttpStatusCode.OK,
                    wishlistController.getWishList(call.currentUserId, limit, offset)
                )
            }

            /**
             * @tag WishList
             * @description Remove a specific product from the user's wishlist
             * @operationId removeFromWishList
             * @query productId (required) Unique identifier of the product to remove
             * @response 200 Product removed from wishlist successfully
             * @response 400 Invalid product ID
             * @security jwtToken
             */
            delete("remove") {
                val productId = call.requireParameters("productId")
                call.respond(
                    HttpStatusCode.OK,
                    wishlistController.removeFromWishList(call.currentUserId, productId.first())
                )
            }

            /**
             * @tag WishList
             * @description Check whether a specific product is in the user's wishlist
             * @operationId checkProductInWishList
             * @query productId (required) Unique identifier of the product to check
             * @response 200 Wishlist check completed successfully
             * @security jwtToken
             */
            get("check") {
                val productId = call.requireParameters("productId")
                call.respond(
                    HttpStatusCode.OK,
                    wishlistController.isProductInWishList(call.currentUserId, productId.first())
                )
            }
        }
    }
