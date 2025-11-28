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
             * @body [CartRequest]
             * @response 200 [Response]
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
             * @query limit (required)
             * @response 200 [Response]
             * @response 400
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
             * @query productId (required)
             * @query quantity (required)
             * @response 200 [Response]
             * @response 400
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
             * @query productId (required)
             * @response 200 [Response]
             * @response 400
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
             * @response 200 [Response]
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