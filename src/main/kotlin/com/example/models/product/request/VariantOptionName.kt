package com.example.models.product.request

import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class VariantOptionName(val variantOptionName: String){
    fun validation(){
        validate(this){
            validate(VariantOptionName::variantOptionName).isNotNull()
        }
    }
}
