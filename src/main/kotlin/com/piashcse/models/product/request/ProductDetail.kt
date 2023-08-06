package com.piashcse.models.product.request

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ProductDetail(@PathParam("productId") val productId: String){
    fun validation() {
        validate(this) {
            validate(ProductDetail::productId).isNotNull().isNotEmpty()
        }
    }
}