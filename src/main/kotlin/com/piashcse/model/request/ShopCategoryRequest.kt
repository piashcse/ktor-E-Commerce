package com.piashcse.model.request

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ShopCategoryRequest(val name: String) {
    fun validation() {
        validate(this) {
            validate(ShopCategoryRequest::name).isNotNull().isNotEmpty()
        }
    }
}
