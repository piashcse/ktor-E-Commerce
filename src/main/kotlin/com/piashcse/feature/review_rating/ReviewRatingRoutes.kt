package com.piashcse.feature.review_rating

import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Route for managing review and rating operations.
 *
 * @param reviewRatingController The controller responsible for handling review and rating-related operations.
 */
fun Route.reviewRatingRoutes(reviewRatingController: ReviewRatingService) {
    route("/review-rating") {

        /**
         * @tag Review Rating
         * @description Retrieve reviews and ratings for a specific product
         * @operationId getReviewRating
         * @query productId (required) Unique identifier of the product
         * @query limit (required) Maximum number of reviews to return
         * @response 200 Product reviews retrieved successfully
         * @response 400 Invalid product ID or limit parameter
         */
        get {
            val (productId, limit) = call.requiredParameters("productId", "limit") ?: return@get
            call.respond(
                ApiResponse.success(
                    reviewRatingController.getReviewRating(productId, limit.toInt()),
                    HttpStatusCode.OK
                )
            )
        }

        authenticate(RoleManagement.CUSTOMER.role) {
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
                    ApiResponse.success(
                        reviewRatingController.addReviewRating(call.currentUser().userId, requestBody),
                        HttpStatusCode.OK
                    )
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
                val (id, review, rating) = call.requiredParameters("id", "review", "rating") ?: return@put
                call.respond(
                    ApiResponse.success(
                        reviewRatingController.updateReviewRating(
                            id,
                            review,
                            rating.toInt()
                        ), HttpStatusCode.OK
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
                val (id) = call.requiredParameters("id") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        reviewRatingController.deleteReviewRating(id), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}