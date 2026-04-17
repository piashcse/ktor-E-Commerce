package com.piashcse.plugin

import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import kotlin.contracts.contract

fun Application.configureSwagger() {
    routing {
        swaggerUI(path = "swagger") {
            info = OpenApiInfo(
                title = "Ktor E-Commerce API",
                version = "1.0.0",
                description = "This is a complete E-Commerce API with user authentication, product management, cart functionality, and order processing.",
                termsOfService = "https://piashcse.github.io/",
                contact= OpenApiInfo.Contact(name = "Mehedi Hassan Piash", email = "piash599@gmail.com"),
                license = OpenApiInfo.License(name = "MIT")
            )
        }
    }
}