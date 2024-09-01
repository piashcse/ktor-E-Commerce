package com.piashcse.models.subcategory

import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddProductSubCategory(
     val categoryId: String,
    val subCategoryName: String
) {
    fun validation() {
        validate(this) {
            validate(AddProductSubCategory::categoryId).isNotNull()
            validate(AddProductSubCategory::subCategoryName).isNotNull()
        }
    }
}