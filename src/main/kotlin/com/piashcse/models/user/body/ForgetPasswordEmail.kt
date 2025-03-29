package com.piashcse.models.user.body

import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ForgetPasswordEmail(val email: String) {
   init {
       validate(this) {
           validate(ForgetPasswordEmail::email).isNotNull().isEmail()
       }
   }
}