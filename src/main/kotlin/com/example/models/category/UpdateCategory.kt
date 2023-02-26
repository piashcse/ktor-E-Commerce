package com.example.models.category

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class UpdateCategory(
    @QueryParam("categoryId") val categoryId: String, @QueryParam("categoryName") val categoryName: String
) {
    fun validation() {
        validate(this) {
            validate(UpdateCategory::categoryId).isNotNull().isNotEmpty()
            validate(UpdateCategory::categoryName).isNotNull().isNotEmpty()
        }
    }
}