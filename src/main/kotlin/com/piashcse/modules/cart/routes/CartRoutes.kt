package com.piashcse.modules.cart.routes

import com.piashcse.modules.cart.controller.CartController
import com.piashcse.database.models.cart.CartRequest
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
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
fun Route.cartRoutes(cartController: CartController) {
    route("cart") {
        /**
         * POST request to add a product to the cart.
         *
         * Accessible by customers only.
         *
         * @param productId The ID of the product to add to the cart.
         * @param quantity The quantity of the product to add to the cart.
         */
        authenticate(RoleManagement.CUSTOMER.role) {
            post({
                tags("Cart")
                summary = "auth[customer]"
                request {
                    body<CartRequest>()
                }
                apiResponse()
            }) {
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
             * GET request to retrieve items from the cart with a specified limit.
             *
             * Accessible by customers only.
             *
             * @param limit The maximum number of items to retrieve from the cart.
             */
            get({
                tags("Cart")
                summary = "auth[customer]"
                request {
                    queryParameter<Int>("limit") {
                        required = true
                    }
                }
                apiResponse()
            }) {
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
             * PUT request to update the quantity of a product in the cart.
             *
             * Accessible by customers only.
             *
             * @param productId The ID of the product to update in the cart.
             * @param quantity The new quantity of the product.
             */
            put({
                tags("Cart")
                summary = "auth[customer]"
                request {
                    queryParameter<String>("productId") {
                        required = true
                    }
                    queryParameter<String>("quantity") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (productId, quantity) = call.requiredParameters("productId", "quantity") ?: return@put
                call.respond(
                    ApiResponse.success(
                        cartController.updateCartQuantity(call.currentUser().userId, productId, quantity.toInt()),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * DELETE request to remove a specific product from the cart.
             *
             * Accessible by customers only.
             *
             * @param productId The ID of the product to remove from the cart.
             */
            delete({
                tags("Cart")
                summary = "auth[customer]"
                request {
                    queryParameter<String>("productId") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (productId) = call.requiredParameters("productId") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        cartController.removeCartItem(call.currentUser().userId, productId),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * DELETE request to clear all items in the cart.
             *
             * Accessible by customers only.
             */
            delete("all", {
                tags("Cart")
                summary = "auth[customer]"
                apiResponse()
            }) {
                call.respond(
                    ApiResponse.success(
                        cartController.clearCart(call.currentUser().userId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}