package com.piashcse.mapper

import com.piashcse.database.entities.ReviewRatingDAO
import com.piashcse.model.response.ReviewRatingResponse

fun ReviewRatingDAO.toReviewRatingResponse() =
    ReviewRatingResponse(
        id = id.value,
        userId = userId.value,
        productId = productId.value,
        reviewText = reviewText,
        rating = rating,
        title = title,
        isVerifiedPurchase = isVerifiedPurchase,
        helpfulCount = helpfulCount,
        notHelpfulCount = notHelpfulCount,
        status = status,
        createdAt = createdAt.toString(),
    )
