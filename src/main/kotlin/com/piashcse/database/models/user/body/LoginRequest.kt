package com.piashcse.database.models.user.body

import com.piashcse.plugins.RoleManagement
import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isIn
import org.valiktor.functions.isNotNull
import org.valiktor.validate

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
                .isIn(RoleManagement.ADMIN.role, RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role)
        }
    }
}
