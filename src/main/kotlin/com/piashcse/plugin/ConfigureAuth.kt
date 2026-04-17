package com.piashcse.plugin

import com.piashcse.constants.UserType
import com.piashcse.feature.auth.JwtConfig
import com.piashcse.model.request.JwtTokenRequest
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

/**
 * Global Authentication Configuration.
 * 
 * Configures the single optimal JWT provider ("jwt-auth") for the entire application.
 * Roles and permissions are handled downstream via [RouteAuthDsl].
 */
fun Application.configureAuth() {
    install(Authentication) {
        jwt("jwt-auth") {
            verifier(JwtConfig.verifier)
            validate { call ->
                val userId = call.payload.getClaim("userId").asString()
                val email = call.payload.getClaim("email").asString()
                val userTypeStr = call.payload.getClaim("userType").asString()

                val userType = try {
                    UserType.valueOf(userTypeStr.uppercase())
                } catch (e: IllegalArgumentException) {
                    return@validate null
                }

                JwtTokenRequest(userId, email, userTypeStr)
            }
        }
    }
}
