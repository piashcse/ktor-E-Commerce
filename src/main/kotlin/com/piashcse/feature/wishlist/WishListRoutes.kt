package com.piashcse.feature.wishlist

import com.piashcse.model.request.WishListRequest
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.*

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Customer wishlist management routes.
 */
fun Route.wishListRoutes() {
    val wishlistRepo: WishListRepository by inject()
    requireRole {
        /**
         * @tag Wishlist
         * @description Add a product to the authenticated user's wishlist
         */
        post {
            call.respondCreated(wishlistRepo.addToWishList(call.currentUserId, call.receive<WishListRequest>().productId))
        }

        /**
         * @tag Wishlist
         * @description Retrieve all items in the user's wishlist
         */
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(wishlistRepo.getWishList(call.currentUserId, limit, offset))
        }

        /**
         * @tag Wishlist
         * @description Remove a specific product from the wishlist
         */
        delete("remove") {
            val productId = call.requireQueryParameter("productId")
            call.respondOk(wishlistRepo.removeFromWishList(call.currentUserId, productId))
        }

        /**
         * @tag Wishlist
         * @description Check if a specific product is in the user's wishlist
         */
        get("check") {
            val productId = call.requireQueryParameter("productId")
            call.respondOk(wishlistRepo.isProductInWishList(call.currentUserId, productId))
        }
    }
}
