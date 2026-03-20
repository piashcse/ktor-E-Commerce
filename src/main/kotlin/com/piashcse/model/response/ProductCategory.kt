package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ProductCategory(
    val id: String,
    val name: String,
    val subCategories: List<ProductSubCategory>,
    val image: String?
)