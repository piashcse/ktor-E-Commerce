package com.piashcse.model.request

import com.piashcse.constants.AppConstants
import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotZero
import org.valiktor.validate

@Serializable
data class ProductWithFilterRequest(
    val limit: Int = AppConstants.Pagination.DEFAULT_LIMIT,
    val offset: Int = AppConstants.Pagination.DEFAULT_OFFSET,
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
