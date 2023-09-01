package com.piashcse.models.subcategory

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddProductSubCategory(
    @QueryParam("categoryId") val categoryId: String,
    @QueryParam("subCategoryName") val subCategoryName: String
) {
    fun validation() {
        validate(this) {
            validate(AddProductSubCategory::categoryId).isNotNull()
            validate(AddProductSubCategory::subCategoryName).isNotNull()
        }
    }
}