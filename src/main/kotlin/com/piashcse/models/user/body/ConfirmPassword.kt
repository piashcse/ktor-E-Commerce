package com.piashcse.models.user.body

import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ConfirmPassword(
    val email: String,
    val verificationCode: String,
    val newPassword: String
) {
    init {
        validate(this) {
            validate(ConfirmPassword::email).isNotNull().isEmail()
            validate(ConfirmPassword::verificationCode).isNotNull()
            validate(ConfirmPassword::newPassword).isNotNull()
        }
    }

    fun validation() {
        validate(this) {
            validate(ConfirmPassword::email).isNotNull().isEmail()
            validate(ConfirmPassword::verificationCode).isNotNull()
            validate(ConfirmPassword::newPassword).isNotNull()
        }
    }
}
