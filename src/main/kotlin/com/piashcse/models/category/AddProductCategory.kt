package com.piashcse.models.category

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddProductCategory(val categoryName: String) {
    fun validation() {
        validate(this) {
            validate(AddProductCategory::categoryName).isNotNull().isNotEmpty()
        }
    }
}