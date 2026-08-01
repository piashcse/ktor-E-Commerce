package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val user: UserResponse?,
    val accessToken: String,
    val refreshToken: String = "",
    val expiresIn: Long = 900,
    val tokenType: String = "Bearer",
)
