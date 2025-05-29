package com.piashcse.model.request

data class UpdateProductRequest(
    val categoryId: String?,
    val subCategoryId: String?,
    val brandId: String?,
    val name: String?,
    val description: String?,
    val stockQuantity: Int?,
    val price: Double?,
    val discountPrice: Double?,
    val status: String?,
    val videoLink: String?,
    val hotDeal: Boolean?,
    val featured: Boolean?,
    val images: List<String>,
)
