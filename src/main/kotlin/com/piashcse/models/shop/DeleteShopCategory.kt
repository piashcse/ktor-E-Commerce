package com.piashcse.models.shop

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class DeleteShopCategory(@QueryParam("shopCategoryId") val shopCategoryId: String){
    fun validation(){
        validate(this){
            validate(DeleteShopCategory::shopCategoryId).isNotNull()
        }
    }
}
