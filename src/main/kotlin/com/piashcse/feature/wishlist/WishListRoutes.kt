@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.wishlist

import com.piashcse.model.request.WishListRequest
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.generalRateLimit
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/**
 * Customer wishlist management routes.
 */
fun Route.wishListRoutes() {
    val wishlistRepo: WishListRepository by inject()
    customerOnlyAuth {
        writeRateLimit {
            /**
             * Add a product to the authenticated user's wishlist
             */
            post {
                call.respondCreated(wishlistRepo.addToWishList(call.currentUserId, call.receive<WishListRequest>().productId))
            }.describe { summary = "Add a product to the authenticated user's wishlist" }

            /**
             * Move a wishlist product into the shopping cart
             */
            post("move-to-cart") {
                val productId = call.requireQueryParameter("productId")
                val quantity = call.request.queryParameters["quantity"]?.toIntOrNull() ?: 1
                call.respondCreated(wishlistRepo.moveToCart(call.currentUserId, productId, quantity))
            }.describe {
                summary = "Move a wishlist product to the cart"
                description = "Adds the product to the cart and removes it from the wishlist"
            }

            delete("remove") {
                val productId = call.requireQueryParameter("productId")
                call.respondOk(wishlistRepo.removeFromWishList(call.currentUserId, productId))
            }.describe { summary = "Remove from wishlist" }
        }

        /**
         * Retrieve all items in the user's wishlist
         */
        generalRateLimit {
            get {
                val (limit, offset) = call.paginateQueryParams()
                call.respondOk(wishlistRepo.getWishList(call.currentUserId, limit, offset))
            }.describe {
                summary = "List wishlist"
                description = "Products with preloaded images"
            }

            /**
             * Check if a specific product is in the user's wishlist
             */
            get("check") {
                val productId = call.requireQueryParameter("productId")
                call.respondOk(wishlistRepo.isProductInWishList(call.currentUserId, productId))
            }.describe { summary = "Check if a specific product is in the user's wishlist" }
        }
    }
}
