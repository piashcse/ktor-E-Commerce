package com.piashcse.models.user.body

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ConfirmPassword(
    @QueryParam("email") val email: String,
    @QueryParam("verificationCode") val verificationCode: String,
    @QueryParam("newPassword") val newPassword: String
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
