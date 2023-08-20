package com.piashcse.models.user.body

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ForgetPasswordEmail(@QueryParam("email") val email: String) {
    fun validation() {
        validate(this) {
            validate(ForgetPasswordEmail::email).isNotNull().isEmail()
        }
    }
}