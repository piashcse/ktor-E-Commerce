package com.piashcse.models.shop

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddShop(
    @QueryParam("shopName") val shopName: String,
    @QueryParam("shopCategoryId") val shopCategoryId: String
) {
    fun validation() {
        validate(this) {
            validate(AddShop::shopName).isNotNull()
            validate(AddShop::shopCategoryId).isNotNull()
        }
    }
}
