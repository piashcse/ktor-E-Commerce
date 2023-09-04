package com.piashcse.models.bands

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddBrand(@QueryParam("brandName") val brandName: String) {
    fun validation() {
        validate(this) {
            validate(AddBrand::brandName).isNotNull()
        }
    }
}