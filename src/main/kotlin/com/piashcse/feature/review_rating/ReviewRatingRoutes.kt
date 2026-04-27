package com.piashcse.feature.review_rating

import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.plugin.customerAuth
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Product review and rating routes.
 */
fun Route.reviewRatingRoutes(reviewRatingService: ReviewRatingService) {
    /**
     * @tag Review Rating
     * @description Retrieve reviews and ratings for a specific product
     */
    get {
        val productId = call.parameters["productId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "productId is required")
        val (limit, offset) = call.paginationParameters()
        call.respond(
            HttpStatusCode.OK,
            reviewRatingService.getReviewRating(productId, limit, offset)
        )
    }

    customerAuth {
        /**
         * @tag Review Rating
         * @description Submit a new review and rating for a product
         */
        post {
            val requestBody = call.receive<ReviewRatingRequest>()
            call.respond(
                HttpStatusCode.OK,
                reviewRatingService.addReviewRating(call.currentUserId, requestBody)
            )
        }

        /**
         * @tag Review Rating
         * @description Update an existing review and rating
         */
        put("{id}") {
            val params = call.requireParameters("id", "review", "rating")
            call.respond(
                HttpStatusCode.OK,
                reviewRatingService.updateReviewRating(
                    params[0],
                    params[1],
                    params[2].toInt()
                )
            )
        }

        /**
         * @tag Review Rating
         * @description Delete a review and rating
         */
        delete("{id}") {
            val id = call.requireParameters("id")
            call.respond(
                HttpStatusCode.OK,
                reviewRatingService.deleteReviewRating(id.first())
            )
        }
    }
}
