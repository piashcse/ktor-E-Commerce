package com.piashcse.models.user.body

import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ForgetPasswordRequest(val email: String) {
   init {
       validate(this) {
           validate(ForgetPasswordRequest::email).isNotNull().isEmail()
       }
   }
}