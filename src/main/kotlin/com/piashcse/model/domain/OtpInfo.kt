package com.piashcse.model.domain

import java.time.LocalDateTime

/**
 * Domain representation of a stored OTP (registration or password reset),
 * exposing only the fields the auth layer needs instead of the underlying DAO.
 */
data class OtpInfo(
    val code: String?,
    val expiry: LocalDateTime?,
)
