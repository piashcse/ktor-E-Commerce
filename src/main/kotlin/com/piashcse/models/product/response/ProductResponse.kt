package com.piashcse.models.product.response

data class ProductResponse(
    val categoryId: String,
    val title: String,
    val productImage:List<String>,
    val description: String,
    val color: String?,
    val size: String?,
    val price: Double,
    val discountPrice: Double?,
    val quantity: Int
)