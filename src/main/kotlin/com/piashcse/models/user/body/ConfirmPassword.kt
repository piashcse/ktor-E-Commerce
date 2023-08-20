package com.piashcse.models.user.body

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ConfirmPassword(
    @QueryParam("email") val email: String,
    @QueryParam("verificationCode") val verificationCode: String,
    @QueryParam("password") val password: String
) {
    fun validation() {
        validate(this) {
            validate(ConfirmPassword::email).isNotNull().isEmail()
            validate(ConfirmPassword::verificationCode).isNotNull()
            validate(ConfirmPassword::password).isNotNull()
        }
    }
}
