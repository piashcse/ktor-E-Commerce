package com.piashcse.modules.wishlist.routes

import com.piashcse.modules.wishlist.controller.WishListController
import com.piashcse.database.models.WisListRequest
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
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
fun Route.wishListRoutes(wishlistController: WishListController) {
    route("wishlist") {
        authenticate(RoleManagement.CUSTOMER.role) {

            /**
             * POST request to add a product to the user's wish list.
             *
             * @param productId The ID of the product to add to the wish list.
             * @response A response indicating the success of adding the product to the wish list.
             */
            post({
                tags("Wish List")
                summary = "auth[customer]"
                request {
                    body<WisListRequest>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<WisListRequest>()
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
             * @param limit The maximum number of products to retrieve from the wish list.
             * @response A response containing the products in the user's wish list.
             */
            get({
                tags("Wish List")
                summary = "auth[customer]"
                request {
                    queryParameter<String>("limit") {
                        required = true
                    }
                }
                apiResponse()
            }) {
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
             * @param productId The ID of the product to remove from the wish list.
             * @response A response indicating the success of removing the product from the wish list.
             */
            delete({
                tags("Wish List")
                summary = "auth[customer]"
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
                        wishlistController.removeFromWishList(call.currentUser().userId, productId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}