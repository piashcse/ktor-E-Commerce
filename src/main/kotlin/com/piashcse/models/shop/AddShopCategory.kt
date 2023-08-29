package com.piashcse.models.shop

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddShopCategory(@QueryParam("shopCategoryName" +
        "") val shopCategoryName: String) {
    fun validation() {
        validate(this) {
            validate(AddShopCategory::shopCategoryName).isNotNull()
        }
    }
}
