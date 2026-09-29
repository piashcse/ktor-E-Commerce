package com.piashcse.model.request

import com.piashcse.utils.validator.ValidationException
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
    // sortBy: price, createdAt, name, best-selling, top-rated, relevance; sortOrder: asc, desc
    val sortBy: String? = null,
    val sortOrder: String? = "desc",
) {
    init {
        validate(this) {
            validate(ProductWithFilterRequest::limit).isNotNull()
            validate(ProductWithFilterRequest::offset).isNotNull()
        }
        // Query-param builders already clamp, but direct constructions must not
        // slip unbounded limits/offsets into OFFSET scans.
        if (limit !in 1..100) {
            throw ValidationException("limit must be between 1 and 100")
        }
        if (offset < 0) {
            throw ValidationException("offset cannot be negative")
        }
    }
}
