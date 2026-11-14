package com.piashcse.model.request

import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val userType: String,
    val mfaCode: String? = null,
) {
    init {
        validate(this) {
            validate(LoginRequest::email).isNotNull().isEmail()
            validate(LoginRequest::password).isNotNull().hasSize(8, 64)
            validate(LoginRequest::userType).isNotNull()
        }
        if (UserType.fromString(userType) == null) {
            throw ValidationException(Message.Validation.INVALID_USER_TYPE)
        }
    }
}
