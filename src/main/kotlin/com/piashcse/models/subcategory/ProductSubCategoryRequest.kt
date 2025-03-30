package com.piashcse.models.subcategory

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ProductSubCategoryRequest(
     val categoryId: String,
    val subCategoryName: String
) {
    fun validation() {
        validate(this) {
            validate(ProductSubCategoryRequest::categoryId).isNotNull().isNotEmpty()
            validate(ProductSubCategoryRequest::subCategoryName).isNotNull().isNotEmpty()
        }
    }
}