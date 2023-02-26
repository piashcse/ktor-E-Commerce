package com.example.models.subcategory

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class UpdateSubCategory(
    @QueryParam("subCategoryId") val subCategoryId: String,
    @QueryParam("subCategoryName") val subCategoryName: String
) {
    fun validation() {
        validate(this) {
            validate(UpdateSubCategory::subCategoryId).isNotNull().isNotEmpty()
            validate(UpdateSubCategory::subCategoryName).isNotNull().isNotEmpty()
        }
    }
}