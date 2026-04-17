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
 * Route for managing review and rating operations.
 *
 * @param reviewRatingController The controller responsible for handling review and rating-related operations.
 */
fun Route.reviewRatingRoutes(reviewRatingController: ReviewRatingService) {

        /**
         * @tag Review Rating
         * @description Retrieve reviews and ratings for a specific product with pagination
         * @operationId getReviewRating
         * @query productId (required) Unique identifier of the product
         * @query limit Maximum number of reviews to return (default 20)
         * @query offset Number of reviews to skip (default 0)
         * @response 200 Product reviews retrieved successfully
         * @response 400 Invalid product ID or limit parameter
         */
        get {
            val productId = call.parameters["productId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "productId is required")
            val (limit, offset) = call.paginationParameters()
            call.respond(
                HttpStatusCode.OK,
                reviewRatingController.getReviewRating(productId, limit, offset)
            )
        }
        customerAuth {
            /**
             * @tag Review Rating
             * @description Submit a new review and rating for a product
             * @operationId addReviewRating
             * @body ReviewRatingRequest Review and rating request with product ID, review text, and rating
             * @response 200 Review and rating added successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<ReviewRatingRequest>()
                call.respond(
                    HttpStatusCode.OK,
                    reviewRatingController.addReviewRating(call.currentUserId, requestBody)
                )
            }

            /**
             * @tag Review Rating
             * @description Update an existing review and rating
             * @operationId updateReviewRating
             * @path id (required) Unique identifier of the review to update
             * @query review (required) Updated review text
             * @query rating (required) Updated rating value (1-5)
             * @response 200 Review and rating updated successfully
             * @response 400 Invalid review ID, review text, or rating parameter
             * @security jwtToken
             */
            put("{id}") {
                val params = call.requireParameters("id", "review", "rating")
                call.respond(
                    HttpStatusCode.OK,
                    reviewRatingController.updateReviewRating(
                        params[0],
                        params[1],
                        params[2].toInt()
                    )
                )
            }

            /**
             * @tag Review Rating
             * @description Permanently delete a review and rating
             * @operationId deleteReviewRating
             * @path id (required) Unique identifier of the review to delete
             * @response 200 Review and rating deleted successfully
             * @response 400 Invalid review ID
             * @security jwtToken
             */
            delete("{id}") {
                val id = call.requireParameters("id")
                call.respond(
                    HttpStatusCode.OK,
                    reviewRatingController.deleteReviewRating(id.first())
                )
            }
        }
    }
