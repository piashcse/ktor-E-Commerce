package com.piashcse.models.user.body

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.piashcse.plugins.RoleManagement
import org.valiktor.functions.*
import org.valiktor.validate

data class LoginBody(
    @QueryParam("email") val email: String,
    @QueryParam("password") val password: String,
    @QueryParam("userType") val userType: String
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
