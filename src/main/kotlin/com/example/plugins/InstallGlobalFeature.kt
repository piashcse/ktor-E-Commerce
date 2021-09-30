package com.example.plugins

import com.example.controller.UserController
import com.example.models.JwtTokenBody
import com.example.utils.JwtConfig
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.freemarker.*
import io.ktor.gson.*
import io.ktor.routing.*

fun Application.installGlobalFeature() {
    install(Compression)
    install(CORS) {
        anyHost()
    }
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
           // serializeNulls()
        }
    }
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
    install(Authentication) {
        /**
         * Setup the JWT authentication to be used in [Routing].
         * If the token is valid, the corresponding [User] is fetched from the database.
         * The [User] can then be accessed in each [ApplicationCall].
         */
        jwt {
            verifier(JwtConfig.verifier)
            realm = "ktor.io"
            validate {
                val userId = it.payload.getClaim("userId").asString()
                val email = it.payload.getClaim("email").asString()
                if (userId != null && email != null) {
                    UserController().jwtVerification(JwtTokenBody(userId, email))
                } else {
                    null
                }
            }
        }
    }
}