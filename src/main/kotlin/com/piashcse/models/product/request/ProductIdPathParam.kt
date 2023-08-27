package com.piashcse.models.product.request

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ProductIdPathParam(@PathParam("productId") val productId: String) {
    fun validation() {
        validate(this) {
            validate(ProductIdPathParam::productId).isNotNull().isNotEmpty()
        }
    }
}