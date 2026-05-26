package com.piashcse.config

object DotEnvConfig {
    // Database configuration
    val dbHost: String get() = DotEnv.get("DB_HOST", "localhost")
    val dbPort: Int get() = DotEnv.getInt("DB_PORT", 5432)
    val dbName: String get() = DotEnv.get("DB_NAME", "ktor-1.0.0")
    val dbUser: String get() = DotEnv.get("DB_USER", "postgres")
    val dbPassword: String 
        get() {
            val pass = DotEnv.get("DB_PASSWORD")
            if (pass.isNullOrEmpty()) {
                throw IllegalStateException("Database password (DB_PASSWORD) must be specified in the environment or .env file.")
            }
            return pass
        }

    // Server configuration
    val serverPort: Int get() = DotEnv.getInt("PORT", 8080)
    val serverHost: String get() = DotEnv.get("HOST", "localhost")

    // JWT configuration
    val jwtSecret: String 
        get() {
            val secret = DotEnv.get("JWT_SECRET")
            if (secret.isNullOrEmpty()) {
                throw IllegalStateException("JWT Secret (JWT_SECRET) must be specified in the environment or .env file.")
            }
            return secret
        }
    val jwtIssuer: String get() = DotEnv.get("JWT_ISSUER", "piashcse")
    val jwtAudience: String get() = DotEnv.get("JWT_AUDIENCE", "ktor-ecommerce")
    val jwtRealm: String get() = DotEnv.get("JWT_REALM", "ktor-ecommerce")

    // CORS configuration
    val allowedOrigins: String get() = DotEnv.get("ALLOWED_ORIGINS", "http://localhost:3000,http://localhost:8080")

    // Email configuration
    val emailHost: String get() = DotEnv.get("EMAIL_HOST", "smtp.gmail.com")
    val emailPort: Int get() = DotEnv.getInt("EMAIL_PORT", 587)
    val emailUsername: String
        get() {
            val user = DotEnv.get("EMAIL_USERNAME")
            if (user.isNullOrEmpty()) {
                throw IllegalStateException("Email username (EMAIL_USERNAME) must be specified in the environment or .env file.")
            }
            return user
        }
    val emailPassword: String
        get() {
            val pass = DotEnv.get("EMAIL_PASSWORD")
            if (pass.isNullOrEmpty()) {
                throw IllegalStateException("Email password (EMAIL_PASSWORD) must be specified in the environment or .env file.")
            }
            return pass
        }
}
