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
         * @query productId The ID of the product to get reviews and ratings. (required)
         * @query limit The maximum number of reviews to retrieve. (required)
         * @response 200 [ApiResponse] A response containing the list of reviews and ratings for the product.
         * @response 400 Bad request if required parameters are missing
         */
        get("/review-rating") {
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
             * @summary auth[customer]
             * @body [ReviewRatingRequest] The body of the request containing review and rating details.
             * @response 200 [ApiResponse] A response indicating the success of the operation.
             */
            post("/review-rating") {
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
             * @summary auth[customer]
             * @path id The ID of the review to update. (required)
             * @query review The updated review content. (required)
             * @query rating The updated rating. (required)
             * @response 200 [ApiResponse] A response containing the updated review and rating.
             * @response 400 Bad request if required parameters are missing
             */
            put("/review-rating/{id}") {
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
             * @summary auth[customer]
             * @path id The ID of the review to delete.
             * @response 200 [ApiResponse] A response indicating the result of the deletion.
             * @response 400 Bad request if id is missing
             */
            delete("/review-rating/{id}") {
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