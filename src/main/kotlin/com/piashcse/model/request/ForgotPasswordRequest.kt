package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class ForgotPasswordRequest(val email: String, val userType: String) {
    init {
        validate(this) {
            validate(ForgotPasswordRequest::email).isNotNull().isEmail()
            validate(ForgotPasswordRequest::userType).isNotNull()
        }
    }
}
