package com.piashcse.plugin

import io.ktor.openapi.*
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Application.configureSwagger() {
    routing {
        swaggerUI(path = "swagger") {
            info =
                OpenApiInfo(
                    title = "Ktor E-Commerce API",
                    version = "1.0.0",
                    description =
                        "Complete E-Commerce API: auth, products, cart, orders. " +
                            "Authenticated endpoints need `Authorization: Bearer <accessToken>` (JWT from POST /api/v1/auth/login).",
                    termsOfService = "https://piashcse.github.io/",
                    contact = OpenApiInfo.Contact(name = "Mehedi Hassan Piash", email = "piash599@gmail.com"),
                    license = OpenApiInfo.License(name = "MIT"),
                )
            // NOTE: ktor-openapi 3.5 does not expose a securityScheme DSL yet.
            // Bearer auth is documented via description + per-route docs; add scheme when upgrading Ktor.
        }
    }
}
