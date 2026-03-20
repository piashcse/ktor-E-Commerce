package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class ShopCategoryRequest(val name: String) {
    fun validation() {
        validate(this) {
            validate(ShopCategoryRequest::name).isNotNull().isNotEmpty()
        }
    }
}
