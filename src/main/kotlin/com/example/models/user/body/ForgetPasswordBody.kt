package com.example.models.user.body

import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ForgetPasswordBody(val email: String) {
    fun validation() {
        validate(this) {
            validate(ForgetPasswordBody::email).isNotNull().isEmail()
        }
    }
}