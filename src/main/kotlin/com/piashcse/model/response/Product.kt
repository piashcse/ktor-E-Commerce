package com.piashcse.model.response

import com.piashcse.constants.ProductStatus

data class Product(
    val id: String,
    val categoryId: String,
    val subCategoryId: String?,
    val brandId: String?,
    val name: String,
    val description: String,
    val minOrderQuantity: Int,
    val stockQuantity: Int,
    val price: Double,
    val discountPrice: Double?,
    val videoLink: String?,
    val hotDeal: Boolean?,
    val featured: Boolean,
    val images: String,
    val status: ProductStatus
)