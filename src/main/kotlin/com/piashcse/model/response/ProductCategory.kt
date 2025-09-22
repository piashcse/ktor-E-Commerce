package com.piashcse.model.response

data class ProductCategory(
    val id: String,
    val name: String,
    val subCategories: List<ProductSubCategory>,
    val image: String?
)