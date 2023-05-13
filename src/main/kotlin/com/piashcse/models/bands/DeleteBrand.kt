package com.piashcse.models.bands

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class DeleteBrand(@QueryParam("brandId") val brandId: String) {
    fun validation() {
        validate(this) {
            validate(DeleteBrand::brandId).isNotNull()
        }
    }
}
