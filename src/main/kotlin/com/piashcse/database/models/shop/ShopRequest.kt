package com.piashcse.database.models.shop

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ShopRequest(
    val name: String,
    val categoryId: String
) {
    fun validation() {
        validate(this) {
            validate(ShopRequest::name).isNotNull().isNotEmpty()
            validate(ShopRequest::categoryId).isNotNull().isNotEmpty()
        }
    }
}
