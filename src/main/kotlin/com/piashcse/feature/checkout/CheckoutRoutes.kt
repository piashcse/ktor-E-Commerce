package com.piashcse.feature.checkout

import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.plugin.RateLimitNames
import com.piashcse.plugin.customerAuth
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.checkoutRoutes() {
    val checkoutService: CheckoutService by inject()
    customerAuth {
        rateLimit(RateLimitName(RateLimitNames.WRITE)) {
            /**
             * @tag Checkout
             * @description Add a new shipping address for the authenticated user
             */
            post("shipping-address") {
                call.respondCreated(checkoutService.createShippingAddress(call.currentUserId, call.receive<ShippingAddressRequest>()))
            }

            /**
             * @tag Checkout
             * @description Update an existing shipping address
             */
            put("shipping-address/{id}") {
                call.respondOk(checkoutService.updateShippingAddress(call.currentUserId, call.requirePathParameter("id"), call.receive<ShippingAddressRequest>()))
            }

            /**
             * @tag Checkout
             * @description Delete a shipping address
             */
            delete("shipping-address/{id}") {
                val id = call.requirePathParameter("id")
                call.respondOk(checkoutService.deleteShippingAddress(call.currentUserId, id))
            }

            /**
             * @tag Checkout
             * @description Get a summary of the checkout (totals) without placing an order
             */
            post("summary") {
                call.respondOk(checkoutService.getCheckoutSummary(call.currentUserId, call.receive<CheckoutRequest>()))
            }

            /**
             * @tag Checkout
             * @description Place a new order from the cart
             */
            post("place-order") {
                call.respondCreated(checkoutService.placeOrder(call.currentUserId, call.receive<CheckoutRequest>()))
            }
        }

        /**
         * @tag Checkout
         * @description Retrieve all shipping addresses for the authenticated user
         */
        get("shipping-address") {
            call.respondOk(checkoutService.getShippingAddresses(call.currentUserId))
        }

        /**
         * @tag Checkout
         * @description Retrieve all available shipping methods
         */
        get("shipping-method") {
            call.respondOk(checkoutService.getShippingMethods())
        }
    }
}
