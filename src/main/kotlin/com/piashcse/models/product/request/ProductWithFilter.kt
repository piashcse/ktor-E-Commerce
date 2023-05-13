package com.piashcse.models.product.request

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotZero
import org.valiktor.validate

data class ProductWithFilter(
    @QueryParam("limit") val limit: Int,
    @QueryParam("offset") val offset: Long,
    @QueryParam("maxPrice") val maxPrice: Double?,
    @QueryParam("minPrice") val minPrice: Double?,
    @QueryParam("categoryId") val categoryId: String?,
    @QueryParam("subCategoryId") val subCategoryId: String?,
    @QueryParam("brandId") val brandId: String?,
) {
    fun validation() {
        validate(this) {
            validate(ProductWithFilter::limit).isNotNull().isNotZero()
            validate(ProductWithFilter::offset).isNotNull()
        }
    }
}
