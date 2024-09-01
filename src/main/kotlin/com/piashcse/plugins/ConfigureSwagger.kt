package com.piashcse.plugins

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.data.AuthScheme
import io.github.smiley4.ktorswaggerui.data.AuthType
import io.github.smiley4.ktorswaggerui.dsl.routing.route
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.swagger.v3.oas.models.media.Schema
import java.io.File

fun Application.configureSwagger() {
    install(SwaggerUI) {
        security {
            // configure a basic-auth security scheme
            securityScheme("jwtToken") {
                type = AuthType.HTTP
                scheme = AuthScheme.BEARER
            }
            // if no other security scheme is specified for a route, the one with this name is used instead
            defaultSecuritySchemeNames("jwtToken")
            // if no other response is documented for "401 Unauthorized", this information is used instead
            defaultUnauthorizedResponse {
                description = "Unauthorized access"
            }
        }
        info {
            title = "Ktor Ecommerce"
            version = "latest"
            description = "API documentation for Ktor Ecommerce App"
            contact {
                name = "PLabs corporation"
                email = "piash599@gmail.com"
            }
        }
        server {
            url = "http://localhost:8080/"
            description = "Development Server"
        }
        server {
            url = "http://ktorecommerce.com/"
            description = "Production Server"
        }
        schemas {
            // overwrite type "File" with custom schema for binary data / multipart form file
            overwrite<File>(Schema<Any>().also {
                it.type = "string"
                it.format = "binary"
            })
        }
        routing {
            // Create a route for the openapi-spec file.
            route("api.json") {
                openApiSpec()
            }
            // Create a route for the swagger-ui using the openapi-spec at "/api.json".
            route("swagger-ui") {
                swaggerUI("/api.json")
            }
            get {
                call.respondRedirect("/swagger-ui/index.html?url=/openapi.json", true)
            }
        }
    }
}