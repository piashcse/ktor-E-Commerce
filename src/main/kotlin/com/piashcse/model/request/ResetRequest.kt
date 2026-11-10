package com.piashcse.model.request

import com.piashcse.utils.validator.requireStrongPassword
import kotlinx.serialization.Serializable
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class ResetRequest(
    val email: String,
    val verificationCode: String,
    val newPassword: String,
    val userType: String,
) {
    init {
        validate(this) {
            validate(ResetRequest::email).isNotNull().isEmail()
            validate(ResetRequest::verificationCode).isNotNull().isNotEmpty()
            validate(ResetRequest::newPassword).isNotNull().isNotEmpty()
            validate(ResetRequest::userType).isNotNull().isNotEmpty()
        }
        newPassword.requireStrongPassword()
    }
}
