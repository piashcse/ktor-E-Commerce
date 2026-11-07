package com.piashcse.model.request

import com.piashcse.constants.ProductStatus
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class UpdateProductRequest(
    val categoryId: String? = null,
    val subCategoryId: String? = null,
    val brandId: String? = null,
    val name: String? = null,
    val description: String? = null,
    @Contextual val price: BigDecimal? = null,
    @Contextual val discountPrice: BigDecimal? = null,
    val clearDiscountPrice: Boolean = false,
    val status: ProductStatus? = null,
    val videoLink: String? = null,
    val hotDeal: Boolean? = null,
    val featured: Boolean? = null,
    val freeShipping: Boolean? = null,
    val images: List<String> = emptyList(),
) {
    init {
        price?.let { require(it > BigDecimal.ZERO) { "Price must be positive" } }
        discountPrice?.let { require(it > BigDecimal.ZERO) { "Discount price must be positive" } }
        if (price != null && discountPrice != null) {
            require(discountPrice < price) { "Discount price must be lower than the regular price" }
        }
    }
}
