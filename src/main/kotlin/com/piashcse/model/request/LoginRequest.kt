package com.piashcse.model.request

import com.piashcse.constants.UserType
import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isIn
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class LoginRequest(
   val email: String,
   val password: String,
   val userType: String
) {
    fun validation() {
        validate(this) {
            validate(LoginRequest::email).isNotNull().isEmail()
            validate(LoginRequest::password).isNotNull().hasSize(4, 10)
            validate(LoginRequest::userType).isNotNull()
                .isIn(UserType.SUPER_ADMIN.name.lowercase(), UserType.ADMIN.name.lowercase(), UserType.SELLER.name.lowercase(), UserType.CUSTOMER.name.lowercase())
        }
    }
}
