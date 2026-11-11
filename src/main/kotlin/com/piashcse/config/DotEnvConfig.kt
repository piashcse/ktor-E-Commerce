package com.piashcse.config

object DotEnvConfig {
    val dbHost: String get() = DotEnv.get("DB_HOST", "localhost")
    val dbPort: Int get() = DotEnv.getInt("DB_PORT", 5432)
    val dbName: String get() = DotEnv.get("DB_NAME", "ktor-ecommerce")
    val dbUser: String get() = DotEnv.get("DB_USER", "postgres")
    val dbPassword: String get() = requireEnv("DB_PASSWORD")
    val serverPort: Int get() = DotEnv.getInt("PORT", 8080)
    val serverHost: String get() = DotEnv.get("HOST", "localhost")
    val jwtSecret: String get() =
        requireEnv("JWT_SECRET").also {
            require(it.length >= 32) { "JWT_SECRET must be at least 32 characters." }
        }
    val jwtIssuer: String get() = DotEnv.get("JWT_ISSUER", "piashcse")
    val jwtAudience: String get() = DotEnv.get("JWT_AUDIENCE", "ktor-ecommerce")
    val jwtRealm: String get() = DotEnv.get("JWT_REALM", "ktor-ecommerce")
    val allowedOrigins: String get() = DotEnv.get("ALLOWED_ORIGINS", "http://localhost:3000,http://localhost:8080")

    /** Absolute storefront base used by sitemap.xml/robots.txt and canonical URLs. */
    val publicBaseUrl: String get() = DotEnv.get("PUBLIC_BASE_URL", "http://localhost:8080").trimEnd('/')

    // Social login
    val googleClientId: String get() = DotEnv.get("GOOGLE_CLIENT_ID", "")

    // WebAuthn / passkeys
    val webauthnRpId: String get() = DotEnv.get("WEBAUTHN_RP_ID", "localhost")
    val webauthnRpName: String get() = DotEnv.get("WEBAUTHN_RP_NAME", "Ktor E-Commerce")
    val webauthnOrigin: String get() = DotEnv.get("WEBAUTHN_ORIGIN", "http://localhost:3000")

    /** Pretty JSON is opt-in (dev only) because it increases payload size and CPU. */
    val jsonPrettyPrint: Boolean get() = DotEnv.getBoolean("JSON_PRETTY_PRINT", false)

    /** Emit HSTS only when the deployment terminates TLS. */
    val forceHttps: Boolean get() = DotEnv.getBoolean("FORCE_HTTPS", false)

    val emailHost: String get() = DotEnv.get("EMAIL_HOST", "smtp.gmail.com")
    val emailPort: Int get() = DotEnv.getInt("EMAIL_PORT", 587)
    val emailUsername: String get() = requireEnv("EMAIL_USERNAME")
    val emailPassword: String get() = requireEnv("EMAIL_PASSWORD")
    val emailSslEnabled: Boolean get() = DotEnv.getBoolean("EMAIL_SSL", false)
    val uploadDir: String get() = DotEnv.get("UPLOAD_DIR", "uploads")

    /** "manual" keeps COD-only behaviour; "stripe" enables card payments + webhooks. */
    val paymentProvider: String get() = DotEnv.get("PAYMENT_PROVIDER", "manual").lowercase()
    val stripeSecretKey: String get() = DotEnv.get("STRIPE_SECRET_KEY", "")
    val stripeWebhookSecret: String get() = DotEnv.get("STRIPE_WEBHOOK_SECRET", "")
    val stripeBaseUrl: String get() = DotEnv.get("STRIPE_BASE_URL", "https://api.stripe.com")
    val stripeWebhookToleranceSeconds: Long get() = DotEnv.getInt("STRIPE_WEBHOOK_TOLERANCE_SECONDS", 300).toLong()

    /** "manual" keeps seller-entered tracking; "shippo" enables rate quotes and labels. */
    val shippingProvider: String get() = DotEnv.get("SHIPPING_PROVIDER", "manual").lowercase()
    val shippoApiToken: String get() = DotEnv.get("SHIPPO_API_TOKEN", "")
    val shippoBaseUrl: String get() = DotEnv.get("SHIPPO_BASE_URL", "https://api.goshippo.com")
    val carrierWebhookSecret: String get() = DotEnv.get("CARRIER_WEBHOOK_SECRET", "")

    /** Optional read-replica JDBC URL; when absent reads use the primary. */
    val dbReadUrl: String? get() = DotEnv.get("DB_READ_URL")
    val dbPoolSize: Int get() = DotEnv.getInt("DB_POOL_SIZE", 10)
    val dbMinIdle: Int get() = DotEnv.getInt("DB_MIN_IDLE", 2)
    val dbConnectionTimeoutMs: Long get() = DotEnv.getInt("DB_CONNECTION_TIMEOUT_MS", 30_000).toLong()

    // Internal token guarding /metrics. Empty (dev/test default) allows loopback only;
    // set METRICS_TOKEN in production to require the X-Metrics-Token header.
    val metricsToken: String get() = DotEnv.get("METRICS_TOKEN", "")

    private fun requireEnv(key: String): String = DotEnv.get(key) ?: error("$key must be specified in the environment or .env file.")
}
