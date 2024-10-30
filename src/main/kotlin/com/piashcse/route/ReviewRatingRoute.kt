package com.piashcse.route

import com.piashcse.controller.ReviewRatingController
import com.piashcse.models.AddReviewRating
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
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
                    queryParameter<String>("offset") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("productId", "limit", "offset")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (productId, limit, offset) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        reviewRatingController.getReviewRating(productId, limit.toInt(), offset.toLong()),
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
                        reviewRatingController.addReviewRating(call.currentUser().userId, requestBody), HttpStatusCode.OK
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
                val requiredParams = listOf("reviewId", "review", "rating")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (reviewId, review, rating) = requiredParams.map { call.parameters[it]!! }
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
                val requiredParams = listOf("reviewId")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (reviewId) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        reviewRatingController.deleteReviewRating(reviewId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}