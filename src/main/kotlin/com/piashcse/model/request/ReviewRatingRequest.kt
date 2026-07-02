package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isBetween
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class ReviewRatingRequest(
    val productId: String,
    val reviewText: String,
    val rating: Int,
) {
    init {
        validate(this) {
            validate(ReviewRatingRequest::productId).isNotNull().isNotEmpty()
            validate(ReviewRatingRequest::reviewText).isNotNull().isNotEmpty()
            validate(ReviewRatingRequest::rating).isNotNull().isBetween(1, 5)
        }
    }
}
