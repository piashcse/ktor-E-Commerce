package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class CartResponse(
    val productId: String,
    val quantity: Int,
    val product: ProductResponse?,
)
