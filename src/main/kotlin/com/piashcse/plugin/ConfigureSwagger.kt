package com.piashcse.plugin

import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Application.configureSwagger() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/openapi.json") {
            version = "4.15.5"
        }
        openAPI(path="openapi", swaggerFile = "openapi/openapi.json")
    }
}