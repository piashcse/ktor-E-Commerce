package com.piashcse.models.user.body

import com.piashcse.plugins.RoleManagement
import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isIn
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class LoginBody(
   val email: String,
   val password: String,
   val userType: String
) {
    fun validation() {
        validate(this) {
            validate(LoginBody::email).isNotNull().isEmail()
            validate(LoginBody::password).isNotNull().hasSize(4, 10)
            validate(LoginBody::userType).isNotNull()
                .isIn(RoleManagement.ADMIN.role, RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role)
        }
    }
}
