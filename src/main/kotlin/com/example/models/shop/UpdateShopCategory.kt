package com.example.models.shop

import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class UpdateShopCategory(val shopCategoryId: String, val shopCategoryName: String) {
    fun validation() {
        validate(this) {
            validate(UpdateShopCategory::shopCategoryId).isNotNull()
            validate(UpdateShopCategory::shopCategoryName).isNotNull()
        }
    }
}
