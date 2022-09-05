package com.example.models.user.body

import org.valiktor.functions.*
import org.valiktor.validate

data class RegistrationBody(val userName: String, val email: String, val password: String, val userType: String) {
    fun validation() {
        validate(this) {
            validate(RegistrationBody::userName).isNotNull()
            validate(RegistrationBody::email).isNotNull().isEmail()
            validate(RegistrationBody::password).isNotNull().hasSize(4, 15)
            validate(RegistrationBody::userType).isNotNull().isIn("1", "2", "3", "4")
        }
    }
}