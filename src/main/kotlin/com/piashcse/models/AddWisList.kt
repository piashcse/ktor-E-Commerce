package com.piashcse.models

import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddWisList(val productId:String){

    fun validation(){
        validate(this){
            validate(AddWisList::productId).isNotNull()
        }
    }
}
