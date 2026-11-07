package com.piashcse.model.request

import com.piashcse.constants.ProductStatus
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProductRequest(
    val categoryId: String? = null,
    val subCategoryId: String? = null,
    val brandId: String? = null,
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val discountPrice: Double? = null,
    val clearDiscountPrice: Boolean = false,
    val status: ProductStatus? = null,
    val videoLink: String? = null,
    val hotDeal: Boolean? = null,
    val featured: Boolean? = null,
    val freeShipping: Boolean? = null,
    val images: List<String> = emptyList(),
) {
    init {
        price?.let { require(it > 0) { "Price must be positive" } }
        discountPrice?.let { require(it > 0) { "Discount price must be positive" } }
        if (price != null && discountPrice != null) {
            require(discountPrice < price) { "Discount price must be lower than the regular price" }
        }
    }
}
