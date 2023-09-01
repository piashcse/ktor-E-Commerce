package com.piashcse.models.subcategory

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class UpdateProductSubCategory(
    @QueryParam("subCategoryId") val subCategoryId: String,
    @QueryParam("subCategoryName") val subCategoryName: String
) {
    fun validation() {
        validate(this) {
            validate(UpdateProductSubCategory::subCategoryId).isNotNull().isNotEmpty()
            validate(UpdateProductSubCategory::subCategoryName).isNotNull().isNotEmpty()
        }
    }
}