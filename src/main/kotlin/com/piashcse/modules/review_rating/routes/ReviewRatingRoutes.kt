package com.piashcse.modules.review_rating.routes

import com.piashcse.modules.review_rating.controller.ReviewRatingController
import com.piashcse.database.models.ReviewRatingRequest
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
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
fun Route.reviewRatingRoutes(reviewRatingController: ReviewRatingController) {
    route("review-rating") {

        /**
         * GET request to retrieve reviews and ratings for a specific product.
         *
         * @param productId The ID of the product to get reviews and ratings.
         * @param limit The maximum number of reviews to retrieve.
         * @response A response containing the list of reviews and ratings for the product.
         */
        get({
            tags("Review Rating")
            request {
                queryParameter<String>("productId") {
                    required = true
                }
                queryParameter<String>("limit") {
                    required = true
                }
            }
            apiResponse()
        }) {
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
             * POST request to add a new review and rating for a product.
             *
             * @param requestBody The body of the request containing review and rating details.
             * @response A response indicating the success of the operation.
             */
            post({
                tags("Review Rating")
                summary = "auth[customer]"
                request {
                    body<ReviewRatingRequest>()
                }
                apiResponse()
            }) {
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
             * PUT request to update an existing review and rating.
             *
             * @param id The ID of the review to update.
             * @param review The updated review content.
             * @param rating The updated rating.
             * @response A response containing the updated review and rating.
             */
            put("{id}", {
                tags("Review Rating")
                summary = "auth[customer]"
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                    queryParameter<String>("review") {
                        required = true
                    }
                    queryParameter<String>("rating") {
                        required = true
                    }
                }
                apiResponse()
            }) {
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
             * DELETE request to remove an existing review and rating.
             *
             * @param id The ID of the review to delete.
             * @response A response indicating the result of the deletion.
             */
            delete("{id}", {
                tags("Review Rating")
                summary = "auth[customer]"
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                }
                apiResponse()
            }) {
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