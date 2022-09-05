package com.example.models.user.body

import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ConfirmPasswordBody(val email:String, val verificationCode:String, val password:String){
   fun validation(){
       validate(this) {
           validate(ConfirmPasswordBody::email).isNotNull().isEmail()
           validate(ConfirmPasswordBody::verificationCode).isNotNull()
           validate(ConfirmPasswordBody::password).isNotNull()
       }
   }
}
