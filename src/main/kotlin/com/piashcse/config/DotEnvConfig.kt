package com.piashcse.config

import com.piashcse.utils.Environment

object DotEnvConfig {
    // Database configuration
    val dbHost: String get() = Environment.get("DB_HOST", "localhost")
    val dbPort: Int get() = Environment.getInt("DB_PORT", 5432)
    val dbName: String get() = Environment.get("DB_NAME", "ktor-1.0.0")
    val dbUser: String get() = Environment.get("DB_USER", "postgres")
    val dbPassword: String get() = Environment.get("DB_PASSWORD", "p123")

    // Server configuration
    val serverPort: Int get() = Environment.getInt("PORT", 8080)
    val serverHost: String get() = Environment.get("HOST", "localhost")

    // JWT configuration
    val jwtSecret: String get() = Environment.get("JWT_SECRET", "zAP5MBA4B4Ijz0MZaS48")
    val jwtIssuer: String get() = Environment.get("JWT_ISSUER", "piashcse")
    val jwtAudience: String get() = Environment.get("JWT_AUDIENCE", "ktor-ecommerce")
    val jwtRealm: String get() = Environment.get("JWT_REALM", "ktor-ecommerce")
    
    // Email configuration
    val emailHost: String get() = Environment.get("EMAIL_HOST", "smtp.gmail.com")
    val emailPort: Int get() = Environment.getInt("EMAIL_PORT", 587)
    val emailUsername: String get() = Environment.get("EMAIL_USERNAME", "")
    val emailPassword: String get() = Environment.get("EMAIL_PASSWORD", "")
}