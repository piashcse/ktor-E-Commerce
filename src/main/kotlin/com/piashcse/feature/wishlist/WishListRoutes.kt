package com.piashcse.feature.wishlist

import com.piashcse.model.request.WishListRequest
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Customer wishlist management routes.
 */
fun Route.wishListRoutes(wishlistController: WishListService) {
    requireRole {
        /**
         * @tag WishList
         * @description Add a product to the authenticated user's wishlist
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
         * @description Retrieve all items in the user's wishlist
         */
        get {
            val (limit, offset) = call.paginationParameters()
            call.respond(
                HttpStatusCode.OK,
                wishlistController.getWishList(call.currentUserId, limit, offset)
            )
        }

        /**
         * @tag WishList
         * @description Remove a specific product from the wishlist
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
         * @description Check if a specific product is in the user's wishlist
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
