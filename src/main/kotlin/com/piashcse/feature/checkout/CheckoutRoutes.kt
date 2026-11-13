@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

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
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.model.response.ShippingAddressResponse
import com.piashcse.plugin.created
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.standardErrors
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.idempotencyKey
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondDeleted
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.ValidationException
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

fun Route.checkoutRoutes() {
    val shippingAddressRepo: ShippingAddressRepository by inject()
    val shippingMethodRepo: ShippingMethodRepository by inject()
    val orderRepo: OrderRepository by inject()
    val consentRepo: ConsentRepository by inject()
    val guestCheckoutService: GuestCheckoutService by inject()
    val featureFlags: FeatureFlags by inject()

    suspend fun consentRequired(): Boolean =
        DotEnv.getBoolean("REQUIRE_POLICY_CONSENT", false) || featureFlags.isEnabled("require_policy_consent")

    suspend fun requireConfiguredConsents(userId: String) {
        if (!consentRequired()) return
        PolicyType.values().filter { it.requiresConsent }.forEach { type ->
            if (!consentRepo.hasUserConsented(userId, type)) {
                throw ValidationException("Consent required for policy: ${type.name}")
            }
        }
    }

    writeRateLimit {
        /**
         * Place an order without an account (feature-flagged)
         */
        post("guest") {
            if (!featureFlags.isEnabled("guest_checkout")) {
                throw ValidationException("Guest checkout is disabled")
            }
            val body = call.receive<GuestCheckoutRequest>()
            if (consentRequired() && !body.consentAccepted) {
                throw ValidationException("Explicit policy consent is required for guest checkout")
            }
            call.respondCreated(guestCheckoutService.placeOrder(body))
        }.describe {
            summary = "Guest checkout"
            description = "Feature-flagged; creates/claims a guest account and places the order"
            requestBody { jsonBody<GuestCheckoutRequest>() }
            responses { created<List<OrderResponse>>("Orders created") }
        }
    }
    customerOnlyAuth {
        writeRateLimit {
            /**
             * Add a new shipping address for the authenticated user
             */
            post("shipping-address") {
                call.respondCreated(shippingAddressRepo.createShippingAddress(call.currentUserId, call.receive<ShippingAddressRequest>()))
            }.describe {
                summary = "Create shipping address"
                requestBody { jsonBody<ShippingAddressRequest>() }
                responses {
                    created<ShippingAddressResponse>("Address created")
                    standardErrors()
                }
            }

            /**
             * Update an existing shipping address
             */
            put("shipping-address/{id}") {
                call.respondOk(
                    shippingAddressRepo.updateShippingAddress(
                        call.currentUserId,
                        call.requirePathParameter("id"),
                        call.receive<ShippingAddressRequest>(),
                    ),
                )
            }.describe {
                summary = "Update shipping address"
                requestBody { jsonBody<ShippingAddressRequest>() }
                responses {
                    ok<ShippingAddressResponse>("Address updated")
                    standardErrors()
                }
            }

            /**
             * Delete a shipping address
             */
            delete("shipping-address/{id}") {
                val id = call.requirePathParameter("id")
                shippingAddressRepo.deleteShippingAddress(call.currentUserId, id)
                call.respondDeleted("Shipping address deleted successfully")
            }.describe { summary = "Delete shipping address" }

            /**
             * Get a summary of the checkout (totals) without placing an order
             */
            post("summary") {
                call.respondOk(orderRepo.getCheckoutSummary(call.currentUserId, call.receive<CheckoutRequest>()))
            }.describe {
                summary = "Checkout summary"
                description = "Totals, tax, shipping and coupon preview; uses variant prices"
                requestBody { jsonBody<CheckoutRequest>() }
                responses {
                    ok<CheckoutSummaryResponse>("Checkout totals")
                    standardErrors()
                }
            }

            /**
             * Place a new order from the cart
             */
            post("place-order") {
                val body = call.receive<CheckoutRequest>()
                val key = call.idempotencyKey(body.idempotencyKey)
                requireConfiguredConsents(call.currentUserId)
                call.respondCreated(orderRepo.placeOrder(call.currentUserId, body.copy(idempotencyKey = key)))
            }.describe {
                summary = "Place order"
                description = "Idempotency-Key header/body supported; splits by shop"
                requestBody { jsonBody<CheckoutRequest>() }
                responses { created<List<OrderResponse>>("Orders created") }
            }
        }

        /**
         * Retrieve all shipping addresses for the authenticated user
         */
        get("shipping-address") {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(shippingAddressRepo.getShippingAddresses(call.currentUserId, limit, offset))
        }.describe { summary = "List shipping addresses" }

        /**
         * Retrieve all available shipping methods
         */
        get("shipping-method") {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(shippingMethodRepo.getShippingMethods(limit, offset))
        }.describe { summary = "List shipping methods" }
    }
}
