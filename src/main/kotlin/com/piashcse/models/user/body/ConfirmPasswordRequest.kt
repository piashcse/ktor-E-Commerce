package com.piashcse.models.user.body

import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ConfirmPasswordRequest(
    val email: String,
    val verificationCode: String,
    val newPassword: String
) {
    fun validation() {
        validate(this) {
            validate(ConfirmPasswordRequest::email).isNotNull().isEmail()
            validate(ConfirmPasswordRequest::verificationCode).isNotNull()
            validate(ConfirmPasswordRequest::newPassword).isNotNull()
        }
    }
}
