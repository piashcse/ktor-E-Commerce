package com.piashcse.models.subcategory

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class DeleteSubCategory(@QueryParam("subCategoryId") val subCategoryId: String) {
    fun validation() {
        validate(this) {
            validate(DeleteSubCategory::subCategoryId).isNotNull()
        }
    }
}
