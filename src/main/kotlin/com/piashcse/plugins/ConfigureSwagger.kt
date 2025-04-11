package com.piashcse.plugins

import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.AuthScheme
import io.github.smiley4.ktoropenapi.config.AuthType
import io.github.smiley4.ktoropenapi.config.SchemaGenerator
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureSwagger() {
    install(OpenApi) {
        info {
            title = "Ktor Ecommerce"
            version = "1.0.0"
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
        schemas {
            // overwrite type "File" with custom schema for binary data
            generator = SchemaGenerator.reflection {
                overwrite(SchemaGenerator.TypeOverwrites.File())
            }
        }
        routing {
            // Create a route for the openapi-spec file.
            route("api.json") {
                openApi()
            }
            // Create a route for the swagger-ui using the openapi-spec at "/api.json".
            route("swagger") {
                swaggerUI("/api.json")
            }
            // Default route for loading swagger
            get {
                call.respondRedirect("/swagger/index.html", true)
            }
        }
    }
}