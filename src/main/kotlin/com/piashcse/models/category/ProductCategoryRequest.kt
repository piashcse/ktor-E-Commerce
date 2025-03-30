package com.piashcse.models.category

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ProductCategoryRequest(val categoryName: String) {
    fun validation() {
        validate(this) {
            validate(ProductCategoryRequest::categoryName).isNotNull().isNotEmpty()
        }
    }
}