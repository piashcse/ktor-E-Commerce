package com.piashcse.feature.cart

import com.piashcse.model.request.CartRequest
import com.piashcse.plugin.RateLimitNames
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Customer shopping cart routes.
 */
fun Route.cartRoutes() {
    val cartRepo: CartRepository by inject()
    requireRole {
        rateLimit(RateLimitName(RateLimitNames.WRITE)) {
            /**
             * @tag Cart
             * @description Add an item to the authenticated user's cart
             */
            post {
                call.respondCreated(call.receive<CartRequest>().let { cartRepo.createCart(call.currentUserId, it.productId, it.quantity) })
            }

            /**
             * @tag Cart
             * @description Set the absolute quantity of an item in the cart (0 removes the item)
             */
            put("update") {
                val result = call.receive<CartRequest>().let { cartRepo.updateCartQuantity(call.currentUserId, it.productId, it.quantity) }
                call.respondOk(result ?: mapOf("message" to "Item removed from cart"))
            }

            /**
             * @tag Cart
             * @description Remove a specific item from the cart
             */
            delete("remove") {
                val productId = call.requireQueryParameter("productId")
                call.respondOk(cartRepo.removeCartItem(call.currentUserId, productId))
            }

            /**
             * @tag Cart
             * @description Remove all items from the authenticated user's cart
             */
            delete("all") {
                call.respondOk(cartRepo.clearCart(call.currentUserId))
            }
        }

        /**
         * @tag Cart
         * @description Retrieve all items in the authenticated user's cart
         */
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(cartRepo.getCartItems(call.currentUserId, limit, offset))
        }

        /**
         * @tag Cart
         * @description Retrieve a summary of the cart (totals, counts)
         */
        get("summary") {
            call.respondOk(cartRepo.getCartSummary(call.currentUserId))
        }
    }
}
