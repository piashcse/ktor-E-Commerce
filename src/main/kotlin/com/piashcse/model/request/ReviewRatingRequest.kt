package com.piashcse.model.request

import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
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
    val images: List<String> = emptyList(),
) {
    init {
        validate(this) {
            validate(ReviewRatingRequest::productId).isNotNull().isNotEmpty()
            validate(ReviewRatingRequest::reviewText).isNotNull().isNotEmpty()
            validate(ReviewRatingRequest::rating).isNotNull().isBetween(1, 5)
        }
        val violations =
            buildList {
                if (images.size > MAX_IMAGES) {
                    add(FieldViolation("images", "At most $MAX_IMAGES images are allowed"))
                }
                if (images.any { it.isBlank() }) {
                    add(FieldViolation("images", "Image URLs must not be blank"))
                }
            }
        if (violations.isNotEmpty()) throw FieldValidationException(violations)
    }

    companion object {
        const val MAX_IMAGES = 8
    }
}
