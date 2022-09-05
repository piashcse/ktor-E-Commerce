package com.example.models.user.body

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class UserId(@QueryParam("userId") val userId: String){
    fun validation(){
        validate(this){
          validate(UserId::userId).isNotNull()
        }
    }
}