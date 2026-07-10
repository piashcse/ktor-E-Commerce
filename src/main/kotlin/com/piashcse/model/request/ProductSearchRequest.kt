package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class ProductSearchRequest(
    val limit: Int,
    val offset: Int = 0,
    val name: String,
    val maxPrice: Double?,
    val minPrice: Double?,
    val categoryId: String?,
    val brandId: String? = null,
    val sortBy: String? = "relevance",
    val sortOrder: String? = "desc",
    val useFuzzy: Boolean? = true,
) {
    init {
        validate(this) {
            validate(ProductSearchRequest::limit).isNotNull()
            validate(ProductSearchRequest::offset).isNotNull()
            validate(ProductSearchRequest::name).isNotNull().isNotEmpty()
        }
    }
}
