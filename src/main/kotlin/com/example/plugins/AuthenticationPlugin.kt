package com.example.plugins

import com.example.models.user.JwtTokenBody
import com.example.utils.AppConstants
import com.example.utils.JwtConfig
import com.example.utils.extension.JwtAuthProvider
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuthentication() {
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
                val userType = it.payload.getClaim("userType").asString()
                if (userType == AppConstants.UserType.CUSTOMER) {
                    JwtTokenBody(userId, email, userType)
                } else null
            }
        }
        jwt(AppConstants.RoleManagement.ADMIN) {
            verifier(JwtConfig.verifier)
            realm = "ktor.io"
            validate {
                val userId = it.payload.getClaim("userId").asString()
                val email = it.payload.getClaim("email").asString()
                val userType = it.payload.getClaim("userType").asString()
                if (userType == AppConstants.UserType.ADMIN) {
                    JwtTokenBody(userId, email, userType)
                } else null
            }
        }
        jwt(AppConstants.RoleManagement.MERCHANT) {
            verifier(JwtConfig.verifier)
            realm = "ktor.io"
            validate {
                val userId = it.payload.getClaim("userId").asString()
                val email = it.payload.getClaim("email").asString()
                val userType = it.payload.getClaim("userType").asString()
                if (userType == AppConstants.UserType.MERCHANT) {
                    JwtTokenBody(userId, email, userType)
                } else null
            }
        }
        /*oauth("auth-oauth-google") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = System.getenv("165959276467-pq9voon4cucgobe583vdbesnbqum7qae.apps.googleusercontent.com"),
                    clientSecret = System.getenv("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"),
                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile"),
                )
            }
            client = httpClient
        }*/
    }
}