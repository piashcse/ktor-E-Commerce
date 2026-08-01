package com.piashcse.model.response

import com.piashcse.constants.UserType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val isVerified: Boolean?,
    var userType: UserType,
    val isActive: Boolean,
    val createdAt: @Contextual LocalDateTime?,
    val updatedAt: @Contextual LocalDateTime?,
)
