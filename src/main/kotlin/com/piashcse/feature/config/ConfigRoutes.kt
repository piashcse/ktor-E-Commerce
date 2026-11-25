@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

@file:Suppress("MatchingDeclarationName")

package com.piashcse.feature.config

import com.piashcse.config.DotEnv
import com.piashcse.constants.AppConstants
import com.piashcse.feature.featureflag.FeatureFlags
import com.piashcse.feature.payment.availablePaymentMethods
import com.piashcse.plugin.generalRateLimit
import com.piashcse.service.payment.*
import com.piashcse.service.payment.PaymentGateway
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.respondOk
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Read-only runtime configuration safe for unauthenticated storefront clients. */
fun Route.configRoutes() {
    val featureFlags: FeatureFlags by inject()
    val gateway: PaymentGateway by inject()
    val fxService: com.piashcse.service.FxService by inject()

    generalRateLimit {
        get {
            call.respondOk(
                PublicConfigResponse(
                    guestCheckout = featureFlags.isEnabled("guest_checkout"),
                    requirePolicyConsent =
                        DotEnv.getBoolean("REQUIRE_POLICY_CONSENT", false) ||
                            featureFlags.isEnabled("require_policy_consent"),
                    supportedCurrencies =
                        com.piashcse.service.CacheService.cache.get<List<String>>("config:currencies")
                            ?: (
                                listOf(AppConstants.DEFAULT_CURRENCY) + fxService.listRates().map { it.targetCurrency }
                            ).distinct().sorted().also {
                                com.piashcse.service.CacheService.cache.set("config:currencies", it, 60)
                            },
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
