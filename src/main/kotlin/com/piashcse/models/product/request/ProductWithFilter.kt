package com.piashcse.models.product.request

import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotZero
import org.valiktor.validate

data class ProductWithFilter(
    val limit: Int,
    val maxPrice: Double?,
    val minPrice: Double?,
    val categoryId: String?,
    val subCategoryId: String?,
    val brandId: String?,
) {
    fun validation() {
        validate(this) {
            validate(ProductWithFilter::limit).isNotNull().isNotZero()
        }
    }
}
