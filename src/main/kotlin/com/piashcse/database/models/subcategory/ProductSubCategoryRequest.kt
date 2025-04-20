package com.piashcse.database.models.subcategory

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ProductSubCategoryRequest(
    val categoryId: String,
    val name: String
) {
    fun validation() {
        validate(this) {
            validate(ProductSubCategoryRequest::categoryId).isNotNull().isNotEmpty()
            validate(ProductSubCategoryRequest::name).isNotNull().isNotEmpty()
        }
    }
}