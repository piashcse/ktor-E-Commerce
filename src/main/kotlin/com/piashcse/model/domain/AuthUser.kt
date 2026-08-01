package com.piashcse.model.domain

import com.piashcse.constants.UserType
import java.time.LocalDateTime

/**
 * Domain representation of a user used by the auth layer. Deliberately free of
 * any persistence types so repository interfaces do not leak DAOs.
 */
data class AuthUser(
    val id: String,
    val email: String,
    val userType: UserType,
    val password: String,
    val isVerified: Boolean,
    val isActive: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
