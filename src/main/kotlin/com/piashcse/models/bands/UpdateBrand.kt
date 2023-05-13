package com.piashcse.models.bands

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class UpdateBrand(@QueryParam("brandId") val brandId: String, @QueryParam("brandName") val brandName: String){
    fun validation() {
        validate(this) {
            validate(UpdateBrand::brandId).isNotNull().isNotEmpty()
            validate(UpdateBrand::brandName).isNotNull().isNotEmpty()
        }
    }
}