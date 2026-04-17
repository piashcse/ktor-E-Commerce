package com.piashcse.model.request

import kotlinx.serialization.Serializable
import com.piashcse.constants.UserType

import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isIn
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class RegisterRequest(val email: String, val password: String, val userType: String) {
    fun validation() {
        validate(this) {
            validate(RegisterRequest::email).isNotNull().isEmail()
            validate(RegisterRequest::password).isNotNull().hasSize(4, 15)
            validate(RegisterRequest::userType).isNotNull()
                .isIn(UserType.ADMIN.name.lowercase(), UserType.CUSTOMER.name.lowercase(), UserType.SELLER.name.lowercase())
        }
    }
}