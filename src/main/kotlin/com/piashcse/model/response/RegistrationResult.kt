package com.piashcse.model.response

import kotlinx.serialization.Serializable

sealed class RegistrationResult {
    abstract val message: String

    @Serializable
    data class Created(
        val id: String,
        val email: String,
        override val message: String,
    ) : RegistrationResult()

    @Serializable
    data class OtpResent(override val message: String) : RegistrationResult()
}
