package com.piashcse.model.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class RegistrationResult {
    abstract val message: String

    @Serializable
    data class Created(
        val id: String,
        val email: String,
        override val message: String,
        @Transient val otp: String? = null,
    ) : RegistrationResult()

    @Serializable
    data class OtpResent(
        val id: String,
        val email: String,
        override val message: String,
        @Transient val otp: String? = null,
    ) : RegistrationResult()
}
