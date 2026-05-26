package com.piashcse.plugin

import com.piashcse.constants.UserType
import com.piashcse.database.entities.BlacklistedTokenDAO
import com.piashcse.database.entities.BlacklistedTokenTable
import com.piashcse.feature.auth.JwtConfig
import com.piashcse.model.request.JwtTokenRequest
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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
            validate { credential ->
                // Check if access token is blacklisted
                val authHeader = this.request.headers[io.ktor.http.HttpHeaders.Authorization]
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    val token = authHeader.substring(7)
                    val isBlacklisted = transaction {
                        BlacklistedTokenDAO.find { BlacklistedTokenTable.token eq token }.firstOrNull() != null
                    }
                    if (isBlacklisted) {
                        return@validate null
                    }
                }

                val userId = credential.payload.getClaim("userId").asString()
                val email = credential.payload.getClaim("email").asString()
                val userTypeStr = credential.payload.getClaim("userType").asString()

                if (runCatching { UserType.valueOf(userTypeStr.uppercase()) }.isFailure) {
                    return@validate null
                }

                JwtTokenRequest(userId, email, userTypeStr)
            }
        }
    }
}
