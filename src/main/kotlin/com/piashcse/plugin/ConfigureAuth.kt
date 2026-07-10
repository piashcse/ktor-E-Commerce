package com.piashcse.plugin

import com.piashcse.constants.UserType
import com.piashcse.database.entities.BlacklistedTokenDAO
import com.piashcse.database.entities.BlacklistedTokenTable
import com.piashcse.feature.auth.JwtConfig
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.utils.extension.query
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq

fun Application.configureAuth() {
    install(Authentication) {
        jwt("jwt-auth") {
            verifier(JwtConfig.verifier)
            validate { credential ->
                val authHeader = this.request.headers[io.ktor.http.HttpHeaders.Authorization]
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    val token = authHeader.substring(7)
                    val isBlacklisted = runBlocking {
                        query { BlacklistedTokenDAO.find { BlacklistedTokenTable.token eq token }.firstOrNull() != null }
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
