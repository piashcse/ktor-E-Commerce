package com.piashcse.feature.checkout

import com.piashcse.config.DotEnv
import com.piashcse.constants.PolicyType
import com.piashcse.feature.consent.ConsentRepository
import com.piashcse.feature.featureflag.FeatureFlags
import com.piashcse.feature.order.OrderRepository
import com.piashcse.feature.shipping_address.ShippingAddressRepository
import com.piashcse.feature.shipping_method.ShippingMethodRepository
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.GuestCheckoutRequest
import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.idempotencyKey
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.checkoutRoutes() {
    val shippingAddressRepo: ShippingAddressRepository by inject()
    val shippingMethodRepo: ShippingMethodRepository by inject()
    val orderRepo: OrderRepository by inject()
    val consentRepo: ConsentRepository by inject()
    val guestCheckoutService: GuestCheckoutService by inject()

    suspend fun requireConfiguredConsents(userId: String) {
        if (!DotEnv.getBoolean("REQUIRE_POLICY_CONSENT", false)) return
        PolicyType.values().filter { it.requiresConsent }.forEach { type ->
            if (!consentRepo.hasUserConsented(userId, type)) {
                throw com.piashcse.utils.validator.ValidationException("Consent required for policy: ${type.name}")
            }
        }
    }

    writeRateLimit {
        /**
         * @tag Checkout
         * @description Place an order without an account (feature-flagged)
         */
        post("guest") {
            if (!FeatureFlags.isEnabled("guest_checkout")) {
                throw com.piashcse.utils.validator.ValidationException("Guest checkout is disabled")
            }
            call.respondCreated(guestCheckoutService.placeOrder(call.receive<GuestCheckoutRequest>()))
        }
    }
    customerOnlyAuth {
        writeRateLimit {
            /**
             * @tag Checkout
             * @description Add a new shipping address for the authenticated user
             */
            post("shipping-address") {
                call.respondCreated(shippingAddressRepo.createShippingAddress(call.currentUserId, call.receive<ShippingAddressRequest>()))
            }

            /**
             * @tag Checkout
             * @description Update an existing shipping address
             */
            put("shipping-address/{id}") {
                call.respondOk(
                    shippingAddressRepo.updateShippingAddress(
                        call.currentUserId,
                        call.requirePathParameter("id"),
                        call.receive<ShippingAddressRequest>(),
                    ),
                )
            }

            /**
             * @tag Checkout
             * @description Delete a shipping address
             */
            delete("shipping-address/{id}") {
                val id = call.requirePathParameter("id")
                shippingAddressRepo.deleteShippingAddress(call.currentUserId, id)
                call.respondOk(com.piashcse.utils.common.MessageResponse("Shipping address deleted successfully"))
            }

            /**
             * @tag Checkout
             * @description Get a summary of the checkout (totals) without placing an order
             */
            post("summary") {
                call.respondOk(orderRepo.getCheckoutSummary(call.currentUserId, call.receive<CheckoutRequest>()))
            }

            /**
             * @tag Checkout
             * @description Place a new order from the cart
             */
            post("place-order") {
                val body = call.receive<CheckoutRequest>()
                val key = call.idempotencyKey(body.idempotencyKey)
                requireConfiguredConsents(call.currentUserId)
                call.respondCreated(orderRepo.placeOrder(call.currentUserId, body.copy(idempotencyKey = key)))
            }
        }

        /**
         * @tag Checkout
         * @description Retrieve all shipping addresses for the authenticated user
         */
        get("shipping-address") {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(shippingAddressRepo.getShippingAddresses(call.currentUserId, limit, offset))
        }

        /**
         * @tag Checkout
         * @description Retrieve all available shipping methods
         */
        get("shipping-method") {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(shippingMethodRepo.getShippingMethods(limit, offset))
        }
    }
}
