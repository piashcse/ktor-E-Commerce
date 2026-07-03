package com.piashcse.model.response

import com.piashcse.constants.ProductStatus
import kotlinx.serialization.Serializable

@Serializable
data class ProductResponse(
    val id: String,
    val userId: String? = null,
    val shopId: String? = null,
    val categoryId: String,
    val subCategoryId: String?,
    val brandId: String?,
    val name: String,
    val description: String,
    val sku: String? = null,
    val barcode: String? = null,
    val weight: Double? = null,
    val dimensions: String? = null,
    val minOrderQuantity: Int,
    val stockQuantity: Int,
    val price: String,
    val discountPrice: String?,
    val discountPercentage: Double? = null,
    val videoLink: String?,
    val hotDeal: Boolean?,
    val featured: Boolean,
    val bestSeller: Boolean = false,
    val newProduct: Boolean = false,
    val freeShipping: Boolean = false,
    val images: List<String>,
    val status: ProductStatus,
    val viewCount: Int = 0,
    val rating: Double = 0.0,
    val totalReviews: Int = 0,
    val totalSales: Int = 0,
)
