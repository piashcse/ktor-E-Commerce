package com.piashcse.model.request

import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.utils.validator.PasswordPolicy
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isIn
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class RegisterRequest(val email: String, val password: String, val userType: String) {
    init {
        validate(this) {
            validate(RegisterRequest::email).isNotNull().isEmail()
            validate(RegisterRequest::password).isNotNull().hasSize(8, 64)
            validate(RegisterRequest::userType).isNotNull()
                .isIn(UserType.CUSTOMER.name.lowercase(), UserType.SELLER.name.lowercase())
        }
        if (!PasswordPolicy.isStrong(password)) {
            throw ValidationException(Message.Validation.WEAK_PASSWORD)
        }
    }
}
