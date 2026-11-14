@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.config

import com.piashcse.config.DotEnv
import com.piashcse.database.entities.SupportedCurrencies
import com.piashcse.feature.featureflag.FeatureFlags
import com.piashcse.plugin.generalRateLimit
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
)

/** Read-only runtime configuration safe for unauthenticated storefront clients. */
fun Route.configRoutes() {
    val featureFlags: FeatureFlags by inject()

    generalRateLimit {
        get {
            call.respondOk(
                PublicConfigResponse(
                    guestCheckout = featureFlags.isEnabled("guest_checkout"),
                    requirePolicyConsent =
                        DotEnv.getBoolean("REQUIRE_POLICY_CONSENT", false) ||
                            featureFlags.isEnabled("require_policy_consent"),
                    supportedCurrencies = SupportedCurrencies.ALL.sorted(),
                ),
            )
        }.describe {
            summary = "Public storefront configuration"
            description = "Feature flags, supported currencies and checkout requirements"
        }
    }
}
