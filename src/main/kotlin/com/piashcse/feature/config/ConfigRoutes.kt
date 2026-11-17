@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

@file:Suppress("MatchingDeclarationName")

package com.piashcse.feature.config

import com.piashcse.config.DotEnv
import com.piashcse.constants.PaymentMethod
import com.piashcse.database.entities.SupportedCurrencies
import com.piashcse.feature.featureflag.FeatureFlags
import com.piashcse.feature.payment.PaymentGateway
import com.piashcse.feature.payment.availablePaymentMethods
import com.piashcse.plugin.generalRateLimit
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.respondOk
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class PublicConfigResponse(
    val guestCheckout: Boolean,
    val requirePolicyConsent: Boolean,
    val supportedCurrencies: List<String>,
    val baseCurrency: String = "USD",
    /** Methods the checkout may offer right now (prevents unpayable orders). */
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val paymentProvider: String = "MANUAL",
    val freeShippingThreshold: String = "75.00",
    val returnWindowDays: Int = 30,
)

/** Read-only runtime configuration safe for unauthenticated storefront clients. */
fun Route.configRoutes() {
    val featureFlags: FeatureFlags by inject()
    val gateway: PaymentGateway by inject()

    generalRateLimit {
        get {
            call.respondOk(
                PublicConfigResponse(
                    guestCheckout = featureFlags.isEnabled("guest_checkout"),
                    requirePolicyConsent =
                        DotEnv.getBoolean("REQUIRE_POLICY_CONSENT", false) ||
                            featureFlags.isEnabled("require_policy_consent"),
                    supportedCurrencies = SupportedCurrencies.ALL.sorted(),
                    paymentMethods = gateway.availablePaymentMethods(),
                    paymentProvider = gateway.provider.name,
                    freeShippingThreshold = Money.str(StorefrontSettings.freeShippingThreshold),
                    returnWindowDays = StorefrontSettings.returnWindowDays,
                ),
            )
        }.describe {
            summary = "Public storefront configuration"
            description = "Feature flags, supported currencies, payment capability and storefront rules"
        }
    }
}
