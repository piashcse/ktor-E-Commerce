package com.piashcse.models.user.body

import com.piashcse.plugins.RoleManagement
import org.valiktor.functions.*
import org.valiktor.validate

data class LoginBody(
   val email: String,
   val password: String,
   val userType: String
) {
    init{
        validate(this) {
            validate(LoginBody::email).isNotNull().isEmail()
            validate(LoginBody::password).isNotNull().hasSize(4, 10)
            validate(LoginBody::userType).isNotNull()
                .isIn(RoleManagement.ADMIN.role, RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role)
        }
    }
}
