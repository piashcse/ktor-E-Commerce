package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ReviewRatingResponse(
    val id: String,
    val userId: String,
    val productId: String,
    val reviewText: String,
    val rating: Int,
)
