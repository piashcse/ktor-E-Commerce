package com.piashcse.mapper

import com.piashcse.database.entities.ReviewRatingDAO
import com.piashcse.model.response.ReviewRatingResponse
import com.piashcse.utils.common.DateTimes.toUtcString

fun ReviewRatingDAO.toReviewRatingResponse(images: List<String> = emptyList()) =
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
        images = images,
        status = status,
        createdAt = createdAt.toUtcString(),
    )
