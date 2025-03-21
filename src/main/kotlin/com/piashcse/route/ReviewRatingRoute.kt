package com.piashcse.route

import com.piashcse.controller.ReviewRatingController
import com.piashcse.models.AddReviewRating
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

fun Route.reviewRatingRoute(reviewRatingController: ReviewRatingController) {
    route("review-rating") {
        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role) {
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
        }
        authenticate(RoleManagement.CUSTOMER.role) {
            post({
                tags("Review Rating")
                request {
                    body<AddReviewRating>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddReviewRating>()
                call.respond(
                    ApiResponse.success(
                        reviewRatingController.addReviewRating(call.currentUser().userId, requestBody),
                        HttpStatusCode.OK
                    )
                )
            }
            put("{reviewId}", {
                tags("Review Rating")
                request {
                    pathParameter<String>("reviewId") {
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
                val (reviewId, review, rating) = call.requiredParameters("reviewId", "review", "rating") ?: return@put
                call.respond(
                    ApiResponse.success(
                        reviewRatingController.updateReviewRating(
                            reviewId,
                            review,
                            rating.toInt()
                        ), HttpStatusCode.OK
                    )
                )
            }
            delete("{reviewId}", {
                tags("Review Rating")
                request {
                    pathParameter<String>("reviewId") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (reviewId) = call.requiredParameters("reviewId") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        reviewRatingController.deleteReviewRating(reviewId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}