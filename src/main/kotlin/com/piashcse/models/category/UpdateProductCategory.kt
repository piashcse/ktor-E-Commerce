package com.piashcse.models.category

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class UpdateProductCategory(
    @QueryParam("categoryId") val categoryId: String, @QueryParam("categoryName") val categoryName: String
) {
    fun validation() {
        validate(this) {
            validate(UpdateProductCategory::categoryId).isNotNull().isNotEmpty()
            validate(UpdateProductCategory::categoryName).isNotNull().isNotEmpty()
        }
    }
}