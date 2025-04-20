package com.piashcse.database.models.user.body

import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ResetRequest(
    val email: String,
    val verificationCode: String,
    val newPassword: String,
    val userType: String
) {
    fun validation() {
        validate(this) {
            validate(ResetRequest::email).isNotNull().isEmail()
            validate(ResetRequest::verificationCode).isNotNull()
            validate(ResetRequest::newPassword).isNotNull()
            validate(ResetRequest::userType).isNotNull()
        }
    }
}
