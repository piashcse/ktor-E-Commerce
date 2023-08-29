package com.piashcse.models.shop

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class UpdateShopCategory(@QueryParam("shopCategoryId") val shopCategoryId: String,@QueryParam("shopCategoryName") val shopCategoryName: String) {
    fun validation() {
        validate(this) {
            validate(UpdateShopCategory::shopCategoryId).isNotNull()
            validate(UpdateShopCategory::shopCategoryName).isNotNull()
        }
    }
}
