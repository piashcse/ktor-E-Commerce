package com.piashcse.feature.cart

import com.piashcse.model.request.CartRequest
import com.piashcse.model.request.RemoveCartItemRequest
import com.piashcse.model.request.UpdateCartItemRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines routes for managing the shopping cart.
 *
 * Accessible by customers only. It includes operations to create, get, update, remove, and clear cart items.
 *
 * @param cartController The controller handling cart-related operations.
 */
fun Route.cartRoutes(cartController: CartService) {
    route("/cart") {
        authenticate(RoleManagement.CUSTOMER.role) {
            /**
             * @tag Cart
             * @description Add a new product item to the user's shopping cart
             * @operationId createCartItem
             * @body CartRequest Cart item request with product ID and quantity
             * @response 200 Item added to cart successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<CartRequest>()
                requestBody.validation()
                call.respond(
                    HttpStatusCode.OK,
                    cartController.createCart(
                        call.currentUserId,
                        requestBody.productId,
                        requestBody.quantity
                    )
                )
            }

            /**
             * @tag Cart
             * @description Retrieve all items in the user's shopping cart
             * @operationId getCartItems
             * @query limit (required) Maximum number of cart items to return
             * @response 200 Cart items retrieved successfully
             * @response 400 Invalid limit parameter
             * @security jwtToken
             */
            get {
                val limit = call.requireParameters("limit")
                call.respond(
                    HttpStatusCode.OK,
                    cartController.getCartItems(
                        call.currentUserId,
                        limit.first().toInt()
                    )
                )
            }

            /**
             * @tag Cart
             * @description Update the quantity of a specific product in the cart
             * @operationId updateCartQuantity
             * @body UpdateCartItemRequest Cart item update request with product ID and quantity
             * @response 200 Cart item quantity updated successfully
             * @security jwtToken
             */
            put("update") {
                val requestBody = call.receive<UpdateCartItemRequest>()
                requestBody.validation()
                call.respond(
                    HttpStatusCode.OK,
                    cartController.updateCartQuantity(call.currentUserId, requestBody.productId, requestBody.quantity)
                )
            }

            /**
             * @tag Cart
             * @description Remove a specific product from the user's shopping cart
             * @operationId removeCartItem
             * @body RemoveCartItemRequest Request with product ID to remove
             * @response 200 Item removed from cart successfully
             * @security jwtToken
             */
            delete("remove") {
                val requestBody = call.receive<RemoveCartItemRequest>()
                requestBody.validation()
                call.respond(
                    HttpStatusCode.OK,
                    cartController.removeCartItem(call.currentUserId, requestBody.productId)
                )
            }

            /**
             * @tag Cart
             * @description Remove all items from the user's shopping cart
             * @operationId clearCart
             * @response 200 Cart cleared successfully
             * @security jwtToken
             */
            delete("all") {
                call.respond(
                    HttpStatusCode.OK,
                    cartController.clearCart(call.currentUserId)
                )
            }

            /**
             * @tag Cart
             * @description Retrieve a summary of items in the user's shopping cart including subtotal and tax
             * @operationId getCartSummary
             * @response 200 Cart summary retrieved successfully
             * @security jwtToken
             */
            get("summary") {
                call.respond(
                    HttpStatusCode.OK,
                    cartController.getCartSummary(call.currentUserId)
                )
            }
        }
    }
}
