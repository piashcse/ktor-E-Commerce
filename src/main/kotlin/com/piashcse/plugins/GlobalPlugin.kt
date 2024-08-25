package com.piashcse.plugins

import com.papsign.ktor.openapigen.route.tags
import com.piashcse.controller.UserController
import com.piashcse.route.userRouteV2
import com.piashcse.utils.Response
import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.data.AuthScheme
import io.github.smiley4.ktorswaggerui.data.AuthType
import io.github.smiley4.ktorswaggerui.dsl.routing.route
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureBasic() {
    install(CallLogging)
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            // serializeNulls()
        }
    }
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
                description = "Username or password is invalid"
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
    }
}