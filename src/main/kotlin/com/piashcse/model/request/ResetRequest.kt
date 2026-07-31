package com.piashcse.model.request

import com.piashcse.constants.Message
import com.piashcse.utils.validator.PasswordPolicy
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.valiktor.functions.isEmail
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
            validate(ResetRequest::verificationCode).isNotNull()
            validate(ResetRequest::newPassword).isNotNull()
            validate(ResetRequest::userType).isNotNull()
        }
        if (!PasswordPolicy.isStrong(newPassword)) {
            throw ValidationException(Message.Validation.WEAK_PASSWORD)
        }
    }
}
