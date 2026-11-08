package com.piashcse.model.response

import com.piashcse.constants.ProductStatus
import kotlinx.serialization.Serializable

@Serializable
data class ProductVariantResponse(
    val id: String,
    val productId: String,
    val sku: String,
    val name: String,
    val price: String,
    val discountPrice: String? = null,
    val discountPercentage: Double? = null,
    val barcode: String? = null,
    val weight: Double? = null,
    val dimensions: String? = null,
    val imageUrl: String? = null,
    val status: ProductStatus = ProductStatus.ACTIVE,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val options: List<ProductVariantOptionResponse> = emptyList(),
)

@Serializable
data class ProductVariantOptionResponse(
    val optionId: String,
    val optionName: String,
    val valueId: String,
    val value: String,
)

@Serializable
data class ProductOptionResponse(
    val id: String,
    val productId: String,
    val name: String,
    val sortOrder: Int = 0,
    val values: List<ProductOptionValueResponse> = emptyList(),
)

@Serializable
data class ProductOptionValueResponse(
    val id: String,
    val optionId: String,
    val value: String,
    val sortOrder: Int = 0,
)
