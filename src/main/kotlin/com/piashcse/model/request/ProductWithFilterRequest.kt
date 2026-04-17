package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class ProductWithFilterRequest(
    val limit: Int,
    val offset: Int = 0,
    val maxPrice: Double?,
    val minPrice: Double?,
    val categoryId: String?,
    val subCategoryId: String?,
    val brandId: String?,
) {
    fun validation() {
        validate(this) {
            validate(ProductWithFilterRequest::limit).isNotNull()
            validate(ProductWithFilterRequest::offset).isNotNull()
        }
    }
}
