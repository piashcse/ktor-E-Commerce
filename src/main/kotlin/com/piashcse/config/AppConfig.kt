package com.piashcse.config

/**
 * Startup configuration validation. Reads the required settings once so a
 * missing/placeholder secret fails the process at boot instead of on the first
 * request that happens to need it.
 */
object AppConfig {
    fun validate() {
        // Accessing these throws for missing required values.
        DotEnvConfig.dbPassword
        DotEnvConfig.jwtSecret
        DotEnvConfig.emailUsername
        DotEnvConfig.emailPassword
        require(DotEnvConfig.publicBaseUrl.startsWith("http")) {
            "PUBLIC_BASE_URL must be an absolute http(s) URL"
        }

        if (DotEnvConfig.paymentProvider == "stripe") {
            require(DotEnvConfig.stripeSecretKey.isNotBlank()) {
                "STRIPE_SECRET_KEY must be set when PAYMENT_PROVIDER=stripe"
            }
            require(DotEnvConfig.stripeWebhookSecret.isNotBlank()) {
                "STRIPE_WEBHOOK_SECRET must be set when PAYMENT_PROVIDER=stripe"
            }
        }
        if (DotEnvConfig.shippingProvider == "shippo") {
            require(DotEnvConfig.shippoApiToken.isNotBlank()) {
                "SHIPPO_API_TOKEN must be set when SHIPPING_PROVIDER=shippo"
            }
        }
    }
}
