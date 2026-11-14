package com.piashcse.model.request

import com.piashcse.constants.UserType
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.requireStrongPassword
import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class RegisterRequest(val email: String, val password: String, val userType: String) {
    init {
        validate(this) {
            validate(RegisterRequest::email).isNotNull().isEmail()
            validate(RegisterRequest::password).isNotNull().hasSize(8, 64)
            validate(RegisterRequest::userType).isNotNull()
        }
        val normalizedType = UserType.fromString(userType)
        if (normalizedType != UserType.CUSTOMER && normalizedType != UserType.SELLER) {
            throw FieldValidationException(
                listOf(FieldViolation("userType", "userType must be customer or seller")),
            )
        }
        password.requireStrongPassword()
    }
}
