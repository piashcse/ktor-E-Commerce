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
             * @summary auth[customer]
             * @body [CartRequest] The product and quantity to add to the cart.
             * @response 200 [ApiResponse] Success response after adding to cart
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
             * @summary auth[customer]
             * @query limit The maximum number of items to retrieve from the cart. (required)
             * @response 200 [ApiResponse] Success response with cart items
             * @response 400 Bad request if limit is missing
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
             * @summary auth[customer]
             * @query productId The ID of the product to update in the cart. (required)
             * @query quantity The new quantity of the product. (required)
             * @response 200 [ApiResponse] Success response after updating quantity
             * @response 400 Bad request if required parameters are missing
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
             * @summary auth[customer]
             * @query productId The ID of the product to remove from the cart. (required)
             * @response 200 [ApiResponse] Success response after removing from cart
             * @response 400 Bad request if productId is missing
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
             * @summary auth[customer]
             * @response 200 [ApiResponse] Success response after clearing cart
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