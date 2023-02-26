package com.example.models.category

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddCategory(val categoryName: String) {
    fun validation() {
        validate(this) {
            validate(AddCategory::categoryName).isNotNull().isNotEmpty()
        }
    }
}