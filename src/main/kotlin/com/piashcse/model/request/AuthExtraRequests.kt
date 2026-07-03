package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class RefreshTokenRequest(val refreshToken: String) {
    init {
        validate(this) {
            validate(RefreshTokenRequest::refreshToken).isNotNull()
        }
    }
}

@Serializable
data class LogoutRequest(val refreshToken: String = "")

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String,
)

@Serializable
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 900,
)
