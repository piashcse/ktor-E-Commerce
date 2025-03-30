package com.piashcse.models.shop

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ShopRequest(
    val shopName: String,
    val shopCategoryId: String
) {
    fun validation() {
        validate(this) {
            validate(ShopRequest::shopName).isNotNull().isNotEmpty()
            validate(ShopRequest::shopCategoryId).isNotNull().isNotEmpty()
        }
    }
}
