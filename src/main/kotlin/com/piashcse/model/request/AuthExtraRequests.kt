package com.piashcse.model.request

import com.piashcse.utils.validator.requireStrongPassword
import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class RefreshTokenRequest(val refreshToken: String) {
    init {
        validate(this) {
            validate(RefreshTokenRequest::refreshToken).isNotNull().isNotEmpty()
        }
    }
}

@Serializable
data class LogoutRequest(val refreshToken: String = "")

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String,
) {
    init {
        validate(this) {
            validate(ChangePasswordRequest::oldPassword).isNotNull().isNotEmpty()
            validate(ChangePasswordRequest::newPassword).isNotNull().isNotEmpty()
        }
        newPassword.requireStrongPassword()
    }
}

@Serializable
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 900,
)
