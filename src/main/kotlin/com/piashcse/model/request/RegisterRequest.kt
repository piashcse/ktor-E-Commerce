package com.piashcse.model.request

import com.piashcse.constants.UserType
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
            validate(RegisterRequest::password).isNotNull().hasSize(8, 100)
            validate(RegisterRequest::userType).isNotNull()
                .isIn(UserType.ADMIN.name.lowercase(), UserType.CUSTOMER.name.lowercase(), UserType.SELLER.name.lowercase())
        }
    }
}
