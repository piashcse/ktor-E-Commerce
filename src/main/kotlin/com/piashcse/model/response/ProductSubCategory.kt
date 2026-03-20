package com.piashcse.model.response

import kotlinx.serialization.Serializable


@Serializable
data class ProductSubCategory(val id: String, val categoryId: String, val name: String, val image: String?)