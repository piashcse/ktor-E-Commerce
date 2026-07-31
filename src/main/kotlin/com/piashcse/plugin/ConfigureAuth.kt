package com.piashcse.plugin

import com.piashcse.constants.AppConstants
import com.piashcse.constants.UserType
import com.piashcse.database.entities.BlacklistedTokenDAO
import com.piashcse.database.entities.BlacklistedTokenTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.feature.auth.JwtConfig
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.service.CacheService
import com.piashcse.utils.extension.query
import io.ktor.http.*
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
                val authHeader = this.request.headers[HttpHeaders.Authorization]
                val token = authHeader?.takeIf { it.startsWith("Bearer ") }?.substring(7)
                if (token != null) {
                    val cacheKey = "blacklisted_token:$token"
                    if (CacheService.cache.get<Boolean>(cacheKey) == true) {
                        authLog.warn("Blacklisted token rejected (from cache)")
                        return@validate null
                    }
                }

                val userId = credential.payload.getClaim("userId").asString()
                val email = credential.payload.getClaim("email").asString()
                val userTypeStr = credential.payload.getClaim("userType").asString()

                if (UserType.fromString(userTypeStr) == null || userId.isBlank()) {
                    authLog.warn("Invalid claims in JWT payload (userId=$userId, userType=$userTypeStr)")
                    return@validate null
                }

                val (isBlacklisted, isUserActive) = query {
                    val blacklisted =
                        token != null &&
                            BlacklistedTokenDAO.find { BlacklistedTokenTable.token eq token }.firstOrNull() != null
                    val user = UserDAO.findById(userId)
                    blacklisted to (user?.isActiveAndVerified() == true)
                }
                if (isBlacklisted) {
                    authLog.warn("Blacklisted token rejected (from database)")
                    return@validate null
                }
                if (!isUserActive) {
                    authLog.warn("JWT rejected: user missing, inactive, or unverified")
                    return@validate null
                }

                JwtTokenRequest(userId, email, userTypeStr)
            }
        }
    }
}
