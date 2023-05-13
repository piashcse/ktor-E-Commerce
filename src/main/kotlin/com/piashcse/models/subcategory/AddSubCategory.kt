package com.piashcse.models.subcategory

import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddSubCategory(val categoryId:String, val subCategoryName: String) {
    fun validation() {
        validate(this) {
            validate(AddSubCategory::categoryId).isNotNull()
            validate(AddSubCategory::subCategoryName).isNotNull()
        }
    }
}