package com.piashcse.models.product.request

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotZero
import org.valiktor.validate

data class ProductSearch(
    val limit: Int,
    val offset: Long,
    val productName: String,
    val maxPrice: Double?,
    val minPrice: Double?,
    val categoryId: String?,
) {
    fun validation() {
        validate(this) {
            validate(ProductSearch::limit).isNotNull().isNotZero()
            validate(ProductSearch::offset).isNotNull()
            validate(ProductSearch::productName).isNotNull().isNotEmpty()
        }
    }
}
