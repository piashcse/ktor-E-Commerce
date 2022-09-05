package com.example.models.user.body

import org.valiktor.functions.*
import org.valiktor.validate

data class LoginBody(val email: String, val password: String, val userType: String) {
    fun validation() {
        validate(this) {
            validate(LoginBody::email).isNotNull().isEmail()
            validate(LoginBody::password).isNotNull().hasSize(4,10)
            validate(LoginBody::userType).isNotNull().isIn("1", "2", "3", "4")
        }
    }
}
