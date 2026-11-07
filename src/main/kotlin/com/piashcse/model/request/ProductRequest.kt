package com.piashcse.model.request

import com.piashcse.constants.ProductStatus
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate
import java.math.BigDecimal

@Serializable
data class ProductRequest(
    val categoryId: String,
    val subCategoryId: String? = null,
    val brandId: String? = null,
    val name: String,
    val description: String,
    val productCode: String? = null,
    val stockQuantity: Int = 0,
    @Contextual val price: BigDecimal,
    @Contextual val discountPrice: BigDecimal? = null,
    val status: ProductStatus? = null,
    val videoLink: String? = null,
    val hotDeal: Boolean = false,
    val featured: Boolean = false,
    val freeShipping: Boolean = false,
    val images: List<String> = emptyList(),
) {
    init {
        validate(this) {
            validate(ProductRequest::categoryId).isNotNull().isNotEmpty()
            validate(ProductRequest::name).isNotNull().isNotEmpty()
            validate(ProductRequest::description).isNotNull().isNotEmpty()
            validate(ProductRequest::price).isNotNull().isGreaterThan(BigDecimal.ZERO)
            validate(ProductRequest::stockQuantity).isGreaterThan(-1)
        }
        discountPrice?.let {
            require(it > BigDecimal.ZERO) { "Discount price must be positive" }
            require(it < price) { "Discount price must be lower than the regular price" }
        }
    }
}
