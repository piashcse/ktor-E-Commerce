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
         * @query productId (required)
         * @query limit (required)
         * @response 200 [Response]
         * @response 400
         */
        get() {
            val (productId, limit) = call.requiredParameters("productId", "limit") ?: return@get
            call.respond(
                ApiResponse.success(
                    reviewRatingController.getReviewRating(productId, limit.toInt()),
                    HttpStatusCode.OK
                )
            )
        }

        // Route for adding, updating, and deleting reviews and ratings
        authenticate(RoleManagement.CUSTOMER.role) {

            // Route for posting a new review and rating
            /**
             * @tag Review Rating
             * @body [ReviewRatingRequest]
             * @response 200 [Response]
             */
            post() {
                val requestBody = call.receive<ReviewRatingRequest>()
                call.respond(
                    ApiResponse.success(
                        reviewRatingController.addReviewRating(call.currentUser().userId, requestBody),
                        HttpStatusCode.OK
                    )
                )
            }

            // Route for updating an existing review and rating
            /**
             * @tag Review Rating
             * @path id
             * @query review
             * @query rating
             * @response 200 [Response]
             * @response 400
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

            // Route for deleting a review and rating
            /**
             * @tag Review Rating
             * @path id
             * @response 200 [Response]
             * @response 400
             */
            delete("/{id}") {
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