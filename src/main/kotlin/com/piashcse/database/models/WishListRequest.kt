package com.piashcse.database.models

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class WishListRequest(val productId:String){
    fun validation(){
        validate(this){
            validate(WishListRequest::productId).isNotNull().isNotEmpty()
        }
    }
}
