package com.piashcse.database.models.product.request

import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotZero
import org.valiktor.validate

data class ProductWithFilterRequest(
    val limit: Int,
    val maxPrice: Double?,
    val minPrice: Double?,
    val categoryId: String?,
    val subCategoryId: String?,
    val brandId: String?,
) {
    fun validation() {
        validate(this) {
            validate(ProductWithFilterRequest::limit).isNotNull().isNotZero()
        }
    }
}
