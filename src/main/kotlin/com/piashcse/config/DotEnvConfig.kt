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
    val emailHost: String get() = DotEnv.get("EMAIL_HOST", "smtp.gmail.com")
    val emailPort: Int get() = DotEnv.getInt("EMAIL_PORT", 587)
    val emailUsername: String get() = requireEnv("EMAIL_USERNAME")
    val emailPassword: String get() = requireEnv("EMAIL_PASSWORD")
    val emailSslEnabled: Boolean get() = DotEnv.getBoolean("EMAIL_SSL", false)
    val uploadDir: String get() = DotEnv.get("UPLOAD_DIR", "uploads")

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
