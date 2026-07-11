package com.piashcse.feature.review_rating

import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.plugin.RateLimitNames
import com.piashcse.plugin.customerAuth
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Product review and rating routes.
 */
fun Route.reviewRatingRoutes() {
    val reviewRatingRepo: ReviewRatingRepository by inject()
    /**
     * @tag Review-Rating
     * @description Retrieve reviews and ratings for a specific product
     */
    get {
        val productId = call.requireQueryParameter("productId")
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(reviewRatingRepo.getReviewRating(productId, limit, offset))
    }

    customerAuth {
        rateLimit(RateLimitName(RateLimitNames.WRITE)) {
            /**
             * @tag Review-Rating
             * @description Submit a new review and rating for a product
             */
            post {
                call.respondCreated(reviewRatingRepo.addReviewRating(call.currentUserId, call.receive<ReviewRatingRequest>()))
            }

            /**
             * @tag Review-Rating
             * @description Update an existing review and rating
             */
            put("{id}") {
                val id = call.requirePathParameter("id")
                val review = call.requireQueryParameter("review")
                val rating = call.requireQueryParameter("rating")
                call.respondOk(reviewRatingRepo.updateReviewRating(call.currentUserId, id, review, rating.toInt()))
            }

            /**
             * @tag Review-Rating
             * @description Delete a review and rating
             */
            delete("{id}") {
                val id = call.requirePathParameter("id")
                call.respondOk(reviewRatingRepo.deleteReviewRating(call.currentUserId, id))
            }
        }
    }
}
