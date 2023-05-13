package com.piashcse.models.shop

import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddShop(val shopName: String, val shopCategoryId: String){
    fun validation(){
        validate(this){
            validate(AddShop::shopName).isNotNull()
            validate(AddShop::shopCategoryId).isNotNull()
        }
    }
}
