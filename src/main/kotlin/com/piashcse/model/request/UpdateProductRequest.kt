package com.piashcse.model.request

import com.piashcse.constants.ProductStatus
import com.piashcse.utils.validator.requirePositivePrice
import com.piashcse.utils.validator.validateDiscountPrice
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
        price?.requirePositivePrice()
        validateDiscountPrice(price, discountPrice)
    }
}
