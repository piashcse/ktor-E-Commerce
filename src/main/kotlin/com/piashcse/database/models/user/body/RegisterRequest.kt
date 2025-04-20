package com.piashcse.database.models.user.body

import com.piashcse.plugins.RoleManagement
import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isIn
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class RegisterRequest(val email: String, val password: String, val userType: String) {
    fun validation() {
        validate(this) {
            validate(RegisterRequest::email).isNotNull().isEmail()
            validate(RegisterRequest::password).isNotNull().hasSize(4, 15)
            validate(RegisterRequest::userType).isNotNull()
                .isIn(RoleManagement.ADMIN.role, RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role)
        }
    }
}