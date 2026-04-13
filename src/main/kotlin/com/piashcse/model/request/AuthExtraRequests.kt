package com.piashcse.model.request

import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class RefreshTokenRequest(val refreshToken: String) {
    fun validate() {
        validate(this) {
            validate(RefreshTokenRequest::refreshToken).isNotNull()
        }
    }
}

data class LogoutRequest(val refreshToken: String = "")

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 86400
)
