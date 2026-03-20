package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class WishListRequest(val productId:String){
    fun validation(){
        validate(this){
            validate(WishListRequest::productId).isNotNull().isNotEmpty()
        }
    }
}
