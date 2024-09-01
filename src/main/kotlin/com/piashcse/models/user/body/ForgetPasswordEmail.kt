package com.piashcse.models.user.body

import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ForgetPasswordEmail(val email: String) {
    fun validation() {
        validate(this) {
            validate(ForgetPasswordEmail::email).isNotNull().isEmail()
        }
    }
}