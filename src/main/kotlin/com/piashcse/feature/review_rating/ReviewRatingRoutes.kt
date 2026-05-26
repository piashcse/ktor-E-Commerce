package com.piashcse.feature.review_rating

import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.plugin.customerAuth
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Product review and rating routes.
 */
fun Route.reviewRatingRoutes(reviewRatingService: ReviewRatingService) {
    /**
     * @tag Review-Rating
     * @description Retrieve reviews and ratings for a specific product
     */
    get {
        val productId = call.requireQueryParameter("productId")
        val (limit, offset) = call.paginateQueryParams()
        call.respond(
            HttpStatusCode.OK,
            reviewRatingService.getReviewRating(productId, limit, offset),
        )
    }

    customerAuth {
        /**
         * @tag Review-Rating
         * @description Submit a new review and rating for a product
         */
        post {
            val requestBody = call.receive<ReviewRatingRequest>()
            call.respond(
                HttpStatusCode.OK,
                reviewRatingService.addReviewRating(call.currentUserId, requestBody),
            )
        }

        /**
         * @tag Review-Rating
         * @description Update an existing review and rating
         */
        put("{id}") {
            val id = call.requirePathParameter("id")
            val review = call.requireQueryParameter("review")
            val rating = call.requireQueryParameter("rating")
            call.respond(
                HttpStatusCode.OK,
                reviewRatingService.updateReviewRating(
                    id,
                    review,
                    rating.toInt(),
                ),
            )
        }

        /**
         * @tag Review-Rating
         * @description Delete a review and rating
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            call.respond(
                HttpStatusCode.OK,
                reviewRatingService.deleteReviewRating(id),
            )
        }
    }
}
