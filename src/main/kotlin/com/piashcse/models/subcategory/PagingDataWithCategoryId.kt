package com.piashcse.models.subcategory

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotZero
import org.valiktor.validate

data class PagingDataWithCategoryId(@QueryParam("categoryId") val categoryId:String, @QueryParam("limit") val limit: Int, @QueryParam("offset") val offset: Long) {
    fun validation() {
        validate(this) {
            validate(PagingDataWithCategoryId::limit).isNotNull().isNotZero()
            validate(PagingDataWithCategoryId::offset).isNotNull()
        }
    }
}
