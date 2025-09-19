package com.piashcse.config

import com.piashcse.utils.DotEnv

object DotEnvConfig {
    // Database configuration
    val dbHost: String get() = DotEnv.get("DB_HOST", "localhost")
    val dbPort: Int get() = DotEnv.getInt("DB_PORT", 5432)
    val dbName: String get() = DotEnv.get("DB_NAME", "ktor-1.0.0")
    val dbUser: String get() = DotEnv.get("DB_USER", "postgres")
    val dbPassword: String get() = DotEnv.get("DB_PASSWORD", "p123")

    // Server configuration
    val serverPort: Int get() = DotEnv.getInt("PORT", 8080)
    val serverHost: String get() = DotEnv.get("HOST", "localhost")

    // JWT configuration
    val jwtSecret: String get() = DotEnv.get("JWT_SECRET", "zAP5MBA4B4Ijz0MZaS48")
    val jwtIssuer: String get() = DotEnv.get("JWT_ISSUER", "piashcse")
    val jwtAudience: String get() = DotEnv.get("JWT_AUDIENCE", "ktor-ecommerce")
    val jwtRealm: String get() = DotEnv.get("JWT_REALM", "ktor-ecommerce")
    
    // Email configuration
    val emailHost: String get() = DotEnv.get("EMAIL_HOST", "smtp.gmail.com")
    val emailPort: Int get() = DotEnv.getInt("EMAIL_PORT", 587)
    val emailUsername: String get() = DotEnv.get("EMAIL_USERNAME", "")
    val emailPassword: String get() = DotEnv.get("EMAIL_PASSWORD", "")
}