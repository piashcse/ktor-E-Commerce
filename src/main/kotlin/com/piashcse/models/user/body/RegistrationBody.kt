package com.piashcse.models.user.body

import com.piashcse.plugins.RoleManagement
import org.valiktor.functions.*
import org.valiktor.validate

data class RegistrationBody(val email: String, val password: String, val userType: String) {
    fun validation() {
        validate(this) {
            validate(RegistrationBody::email).isNotNull().isEmail()
            validate(RegistrationBody::password).isNotNull().hasSize(4, 15)
            validate(RegistrationBody::userType).isNotNull()
                .isIn(RoleManagement.ADMIN.role, RoleManagement.USER.role, RoleManagement.SELLER.role)
        }
    }
}