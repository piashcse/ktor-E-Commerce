package com.piashcse.feature.review_rating

import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.plugin.customerAuth
import com.piashcse.plugin.writeRateLimit
import com.piashcse.service.CacheService
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
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
        writeRateLimit {
            /**
             * @tag Review-Rating
             * @description Submit a new review and rating for a product
             */
            post {
                val result = reviewRatingRepo.addReviewRating(call.currentUserId, call.receive<ReviewRatingRequest>())
                CacheService.cache.invalidatePattern("products:.*")
                call.respondCreated(result)
            }

            /**
             * @tag Review-Rating
             * @description Update an existing review and rating
             */
            put("{id}") {
                val id = call.requirePathParameter("id")
                val review = call.requireQueryParameter("review")
                val rating = call.requireQueryParameter("rating")
                val result = reviewRatingRepo.updateReviewRating(call.currentUserId, id, review, rating.toInt())
                CacheService.cache.invalidatePattern("products:.*")
                call.respondOk(result)
            }

            /**
             * @tag Review-Rating
             * @description Delete a review and rating
             */
            delete("{id}") {
                val id = call.requirePathParameter("id")
                reviewRatingRepo.deleteReviewRating(call.currentUserId, id)
                CacheService.cache.invalidatePattern("products:.*")
                call.respondOk(com.piashcse.utils.common.MessageResponse("Review deleted successfully"))
            }

            post("{id}/helpful") {
                val id = call.requirePathParameter("id")
                val helpful = call.request.queryParameters["helpful"]?.toBoolean() ?: true
                call.respondOk(reviewRatingRepo.markHelpful(call.currentUserId, id, helpful))
            }
        }
    }
}
