@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.cart

import com.piashcse.database.entities.Cart
import com.piashcse.model.request.CartRequest
import com.piashcse.plugin.created
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.generalRateLimit
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/**
 * Customer shopping cart routes.
 */
fun Route.cartRoutes() {
    val cartRepo: CartRepository by inject()
    customerOnlyAuth {
        writeRateLimit {
            /**
             * Add an item to the authenticated user's cart
             */
            post {
                call.respondCreated(
                    call.receive<CartRequest>().let {
                        cartRepo.createCart(call.currentUserId, it.productId, it.quantity, it.variantId)
                    },
                )
            }.describe {
                summary = "Add item to cart"
                description = "variantId is required when the product has variants"
                requestBody { jsonBody<CartRequest>() }
                responses { created<Cart>("Cart line") }
            }

            /**
             * Set the absolute quantity of an item in the cart (0 removes the item)
             */
            put("update") {
                val result =
                    call.receive<CartRequest>().let {
                        cartRepo.updateCartQuantity(call.currentUserId, it.productId, it.quantity, it.variantId)
                    }
                call.respondOk(result ?: MessageResponse("Item removed from cart"))
            }.describe {
                summary = "Update cart quantity"
                description = "Absolute quantity; 0 removes the line"
                requestBody { jsonBody<CartRequest>() }
            }

            /**
             * Remove a specific item from the cart
             */
            delete("remove") {
                val productId = call.requireQueryParameter("productId")
                val variantId = call.request.queryParameters["variantId"]
                call.respondOk(cartRepo.removeCartItem(call.currentUserId, productId, variantId))
            }.describe {
                summary = "Remove cart item"
                description = "Pass variantId for variant lines"
            }

            /**
             * Remove all items from the authenticated user's cart
             */
            delete("all") {
                cartRepo.clearCart(call.currentUserId)
                call.respondOk(com.piashcse.utils.common.MessageResponse("Cart cleared successfully"))
            }.describe { summary = "Clear cart" }
        }

        /**
         * Retrieve all items in the authenticated user's cart
         */
        generalRateLimit {
            get {
                val (limit, offset) = call.paginateQueryParams()
                call.respondOk(cartRepo.getCartItems(call.currentUserId, limit, offset))
            }.describe { summary = "Get cart" }

            /**
             * Retrieve a summary of the cart (totals, counts)
             */
            get("summary") {
                call.respondOk(cartRepo.getCartSummary(call.currentUserId))
            }.describe {
                summary = "Cart summary"
                description = "Variant-aware prices and stock; estimated tax included"
            }
        }
    }
}
