package com.piashcse.model.request

import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ForgetPasswordRequest(val email: String, val userType: String) {
   init {
       validate(this) {
           validate(ForgetPasswordRequest::email).isNotNull().isEmail()
           validate(ForgetPasswordRequest::userType).isNotNull()
       }
   }
}
