package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isLessThanOrEqualTo
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotEmpty
import org.valiktor.validate

@Serializable
data class ReviewRatingRequest(
    val productId: String,
    val reviewText: String,
    val rating: Int
) {
    fun validation() {
        validate(this) {
            validate(ReviewRatingRequest::productId).isNotNull().isNotEmpty()
            validate(ReviewRatingRequest::reviewText).isNotNull().isNotEmpty()
            validate(ReviewRatingRequest::rating).isGreaterThan(0).isLessThanOrEqualTo(5)
        }
    }
}
