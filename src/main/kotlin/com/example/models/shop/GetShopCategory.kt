package com.example.models.shop

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotZero
import org.valiktor.validate

data class GetShopCategory(@QueryParam("offset") val offset: Int, @QueryParam("limit") val limit: Int) {
    fun validation() {
        validate(this) {
            validate(GetShopCategory::offset).isNotNull()
            validate(GetShopCategory::limit).isNotNull().isNotZero()
        }
    }
}
