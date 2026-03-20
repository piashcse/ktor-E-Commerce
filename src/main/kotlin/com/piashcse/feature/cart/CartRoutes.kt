package com.piashcse.feature.cart

import com.piashcse.model.request.CartRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
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
                    ApiResponse.success(
                        cartController.createCart(
                            call.currentUser().userId,
                            requestBody.productId,
                            requestBody.quantity
                        ), HttpStatusCode.OK
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
                val (limit) = call.requiredParameters("limit") ?: return@get
                call.respond(
                    ApiResponse.success(
                        cartController.getCartItems(
                            call.currentUser().userId,
                            limit.toInt()
                        ), HttpStatusCode.OK
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
                val (productId, quantity) = call.requiredParameters("productId", "quantity") ?: return@put
                call.respond(
                    ApiResponse.success(
                        cartController.updateCartQuantity(call.currentUser().userId, productId, quantity.toInt()),
                        HttpStatusCode.OK
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
                val (productId) = call.requiredParameters("productId") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        cartController.removeCartItem(call.currentUser().userId, productId),
                        HttpStatusCode.OK
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
                    ApiResponse.success(
                        cartController.clearCart(call.currentUser().userId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}