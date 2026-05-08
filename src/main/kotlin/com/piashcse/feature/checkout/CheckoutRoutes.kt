package com.piashcse.feature.checkout

import com.piashcse.feature.order.OrderService
import com.piashcse.feature.shipping_address.ShippingAddressService
import com.piashcse.feature.shipping_method.ShippingMethodService
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.plugin.customerAuth
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.checkoutRoutes(
    shippingAddressService: ShippingAddressService,
    shippingMethodService: ShippingMethodService,
    orderService: OrderService,
) {
    customerAuth {
        /**
         * @tag Checkout
         * @description Add a new shipping address for the authenticated user
         */
        post("shipping-address") {
            val requestBody = call.receive<ShippingAddressRequest>()
            requestBody.validation()
            call.respond(
                HttpStatusCode.Created,
                shippingAddressService.createShippingAddress(call.currentUserId, requestBody),
            )
        }

        /**
         * @tag Checkout
         * @description Retrieve all shipping addresses for the authenticated user
         */
        get("shipping-address") {
            call.respond(
                HttpStatusCode.OK,
                shippingAddressService.getShippingAddresses(call.currentUserId),
            )
        }

        /**
         * @tag Checkout
         * @description Update an existing shipping address
         */
        put("shipping-address/{id}") {
            val (id) = call.requireParameters("id")
            val requestBody = call.receive<ShippingAddressRequest>()
            requestBody.validation()
            call.respond(
                HttpStatusCode.OK,
                shippingAddressService.updateShippingAddress(call.currentUserId, id, requestBody),
            )
        }

        /**
         * @tag Checkout
         * @description Delete a shipping address
         */
        delete("shipping-address/{id}") {
            val (id) = call.requireParameters("id")
            call.respond(
                HttpStatusCode.OK,
                shippingAddressService.deleteShippingAddress(call.currentUserId, id),
            )
        }

        /**
         * @tag Checkout
         * @description Retrieve all available shipping methods
         */
        get("shipping-method") {
            call.respond(
                HttpStatusCode.OK,
                shippingMethodService.getShippingMethods(),
            )
        }

        /**
         * @tag Checkout
         * @description Get a summary of the checkout (totals) without placing an order
         */
        post("summary") {
            val requestBody = call.receive<CheckoutRequest>()
            requestBody.validation()
            call.respond(
                HttpStatusCode.OK,
                orderService.getCheckoutSummary(call.currentUserId, requestBody),
            )
        }

        /**
         * @tag Checkout
         * @description Place a new order from the cart
         */
        post("place-order") {
            val requestBody = call.receive<CheckoutRequest>()
            requestBody.validation()
            call.respond(
                HttpStatusCode.Created,
                orderService.placeOrder(call.currentUserId, requestBody),
            )
        }
    }
}
