package com.piashcse.feature.cart

import com.piashcse.model.request.CartRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
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
                call.respond(
                    ApiResponse.ok(
                        cartController.createCart(
                            call.currentUserId,
                            requestBody.productId,
                            requestBody.quantity
                        )
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
                    ApiResponse.ok(
                        cartController.getCartItems(
                            call.currentUserId,
                            limit.first().toInt()
                        )
                    )
                )
            }

            /**
             * @tag Cart
             * @description Update the quantity of a specific product in the cart
             * @operationId updateCartQuantity
             * @query productId (required) Unique identifier of the product
             * @query quantity (required) New quantity value for the product
             * @response 200 Cart item quantity updated successfully
             * @response 400 Invalid product ID or quantity parameter
             * @security jwtToken
             */
            put("update") {
                val params = call.requireParameters("productId", "quantity")
                call.respond(
                    ApiResponse.ok(
                        cartController.updateCartQuantity(call.currentUserId, params[0], params[1].toInt())
                    )
                )
            }

            /**
             * @tag Cart
             * @description Remove a specific product from the user's shopping cart
             * @operationId removeCartItem
             * @query productId (required) Unique identifier of the product to remove
             * @response 200 Item removed from cart successfully
             * @response 400 Invalid product ID
             * @security jwtToken
             */
            delete("remove") {
                val productId = call.requireParameters("productId")
                call.respond(
                    ApiResponse.ok(
                        cartController.removeCartItem(call.currentUserId, productId.first())
                    )
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
                    ApiResponse.ok(
                        cartController.clearCart(call.currentUserId)
                    )
                )
            }
        }
    }
}