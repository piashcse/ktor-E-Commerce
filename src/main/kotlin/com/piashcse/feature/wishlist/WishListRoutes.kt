package com.piashcse.feature.wishlist

import com.piashcse.model.request.WishListRequest
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginationParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Customer wishlist management routes.
 */
fun Route.wishListRoutes(wishlistService: WishListService) {
    requireRole {
        /**
         * @tag Wishlist
         * @description Add a product to the authenticated user's wishlist
         */
        post {
            val requestBody = call.receive<WishListRequest>()
            requestBody.validation()
            call.respond(
                HttpStatusCode.OK,
                wishlistService.addToWishList(call.currentUserId, requestBody.productId),
            )
        }

        /**
         * @tag Wishlist
         * @description Retrieve all items in the user's wishlist
         */
        get {
            val (limit, offset) = call.paginationParameters()
            call.respond(
                HttpStatusCode.OK,
                wishlistService.getWishList(call.currentUserId, limit, offset),
            )
        }

        /**
         * @tag Wishlist
         * @description Remove a specific product from the wishlist
         */
        delete("remove") {
            val productId = call.requireQueryParameter("productId")
            call.respond(
                HttpStatusCode.OK,
                wishlistService.removeFromWishList(call.currentUserId, productId),
            )
        }

        /**
         * @tag Wishlist
         * @description Check if a specific product is in the user's wishlist
         */
        get("check") {
            val productId = call.requireQueryParameter("productId")
            call.respond(
                HttpStatusCode.OK,
                wishlistService.isProductInWishList(call.currentUserId, productId),
            )
        }
    }
}
