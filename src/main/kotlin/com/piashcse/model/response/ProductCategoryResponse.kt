package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ProductCategoryResponse(
    val id: String,
    val name: String,
    val subCategories: List<ProductSubCategoryResponse>,
    val image: String?
)