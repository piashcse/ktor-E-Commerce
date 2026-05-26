package com.piashcse.feature.cart

import com.piashcse.model.request.CartRequest
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginationParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Customer shopping cart routes.
 */
fun Route.cartRoutes(cartService: CartService) {
    requireRole {
        /**
         * @tag Cart
         * @description Add an item to the authenticated user's cart
         */
        post {
            val requestBody = call.receive<CartRequest>()
            call.respond(
                HttpStatusCode.OK,
                cartService.createCart(call.currentUserId, requestBody.productId, requestBody.quantity),
            )
        }

        /**
         * @tag Cart
         * @description Retrieve all items in the authenticated user's cart
         */
        get {
            val (limit, offset) = call.paginationParameters()
            call.respond(
                HttpStatusCode.OK,
                cartService.getCartItems(call.currentUserId, limit, offset),
            )
        }

        /**
         * @tag Cart
         * @description Update the quantity of an item in the cart
         */
        put("update") {
            val requestBody = call.receive<CartRequest>()
            call.respond(
                HttpStatusCode.OK,
                cartService.updateCartQuantity(call.currentUserId, requestBody.productId, requestBody.quantity),
            )
        }

        /**
         * @tag Cart
         * @description Remove a specific item from the cart
         */
        delete("remove") {
            val productId = call.requireQueryParameter("productId")
            call.respond(
                HttpStatusCode.OK,
                cartService.removeCartItem(call.currentUserId, productId),
            )
        }

        /**
         * @tag Cart
         * @description Remove all items from the authenticated user's cart
         */
        delete("all") {
            call.respond(
                HttpStatusCode.OK,
                cartService.clearCart(call.currentUserId),
            )
        }

        /**
         * @tag Cart
         * @description Retrieve a summary of the cart (totals, counts)
         */
        get("summary") {
            call.respond(
                HttpStatusCode.OK,
                cartService.getCartSummary(call.currentUserId),
            )
        }
    }
}
