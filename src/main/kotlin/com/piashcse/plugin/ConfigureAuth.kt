package com.piashcse.plugin

import com.piashcse.constants.AppConstants
import com.piashcse.constants.UserType
import com.piashcse.database.entities.BlacklistedTokenDAO
import com.piashcse.database.entities.BlacklistedTokenTable
import com.piashcse.feature.auth.JwtConfig
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.utils.extension.query
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.jetbrains.exposed.v1.core.eq
import org.slf4j.LoggerFactory

private val authLog = LoggerFactory.getLogger("com.piashcse.plugin.ConfigureAuth")

fun Application.configureAuth() {
    install(Authentication) {
        jwt(AppConstants.Authentication.JWT_AUTHENTICATOR) {
            verifier(JwtConfig.verifier)
            realm = "ecom-api"
            validate { credential ->
                val authHeader = this.request.headers[io.ktor.http.HttpHeaders.Authorization]
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    val token = authHeader.substring(7)
                    val isBlacklisted = query { BlacklistedTokenDAO.find { BlacklistedTokenTable.token eq token }.firstOrNull() != null }
                    if (isBlacklisted) {
                        authLog.warn("Blacklisted token rejected for user")
                        return@validate null
                    }
                }

                val userId = credential.payload.getClaim("userId").asString()
                val email = credential.payload.getClaim("email").asString()
                val userTypeStr = credential.payload.getClaim("userType").asString()

                if (UserType.fromString(userTypeStr) == null) {
                    authLog.warn("Invalid userType in JWT payload: $userTypeStr")
                    return@validate null
                }

                JwtTokenRequest(userId, email, userTypeStr)
            }
        }
    }
}
