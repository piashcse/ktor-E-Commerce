@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.review_rating

import com.piashcse.constants.ReviewStatus
import com.piashcse.model.request.BulkIdsRequest
import com.piashcse.model.request.BulkResultResponse
import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.customerAuth
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.writeRateLimit
import com.piashcse.service.CatalogCache
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondCsv
import com.piashcse.utils.extension.respondDeleted
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class ReviewImagesRequest(val images: List<String>)

@Serializable
data class ReviewModerationRequest(
    val status: ReviewStatus,
    val reason: String? = null,
)

/**
 * Product review and rating routes.
 */
fun Route.reviewRatingRoutes() {
    val reviewRatingRepo: ReviewRatingRepository by inject()
    /**
     * Retrieve reviews and ratings for a specific product
     */
    get {
        val productId = call.requireQueryParameter("productId")
        val (limit, offset) = call.pageRequest()
        call.respondOk(reviewRatingRepo.getReviewRating(productId, limit, offset))
    }.describe { summary = "Retrieve reviews and ratings for a specific product" }

    /**
     * Aggregate rating distribution for a product.
     */
    get("summary") {
        val productId = call.requireQueryParameter("productId")
        call.respondOk(reviewRatingRepo.getReviewSummary(productId))
    }.describe { summary = "Rating distribution for a product" }

    customerAuth {
        writeRateLimit {
            /**
             * Submit a new review and rating for a product
             */
            post {
                val result = reviewRatingRepo.addReviewRating(call.currentUserId, call.receive<ReviewRatingRequest>())
                CatalogCache.invalidate()
                call.respondCreated(result)
            }.describe { summary = "Submit a new review and rating for a product" }

            /**
             * Update an existing review and rating
             */
            put("{id}") {
                val id = call.requirePathParameter("id")
                val review = call.requireQueryParameter("review")
                val rating = call.requireQueryParameter("rating")
                val result = reviewRatingRepo.updateReviewRating(call.currentUserId, id, review, rating.toInt())
                CatalogCache.invalidate()
                call.respondOk(result)
            }.describe { summary = "Update an existing review and rating" }

            put("{id}/images") {
                val id = call.requirePathParameter("id")
                val body = call.receive<ReviewImagesRequest>()
                val result = reviewRatingRepo.updateReviewImages(call.currentUserId, id, body.images)
                CatalogCache.invalidate()
                call.respondOk(result)
            }.describe {
                summary = "Replace review images"
                description = "At most 8 image URLs"
                requestBody { jsonBody<ReviewImagesRequest>() }
                responses { ok<com.piashcse.model.response.ReviewRatingResponse>("Updated review") }
            }

            /**
             * Delete a review and rating
             */
            delete("{id}") {
                val id = call.requirePathParameter("id")
                reviewRatingRepo.deleteReviewRating(call.currentUserId, id)
                CatalogCache.invalidate()
                call.respondDeleted("Review deleted successfully")
            }.describe { summary = "Delete a review and rating" }

            post("{id}/helpful") {
                val id = call.requirePathParameter("id")
                val helpful = call.request.queryParameters["helpful"]?.toBoolean() ?: true
                call.respondOk(reviewRatingRepo.markHelpful(call.currentUserId, id, helpful))
            }.describe {
                summary = "Vote a review helpful"
                description = "helpful=false removes the vote"
            }
        }
    }
}

/** Admin review moderation: list by status and hide/flag/restore reviews. */
fun Route.reviewAdminRoutes() {
    val reviewRatingRepo: ReviewRatingRepository by inject()
    get {
        val (limit, offset) = call.pageRequest()
        val status = call.request.queryParameters["status"]?.parseEnum<ReviewStatus>("status")
        call.respondOk(reviewRatingRepo.getReviews(status, limit, offset))
    }.describe {
        summary = "Admin: list reviews"
        description = "Optional status filter"
    }
    get("export") {
        call.respondCsv(
            "reviews.csv",
            listOf("id", "user_id", "product_id", "rating", "status", "title", "created_at"),
            fetch = { limit, offset ->
                reviewRatingRepo.getReviews(
                    call.request.queryParameters["status"]?.parseEnum<ReviewStatus>("status"),
                    limit,
                    offset,
                ).data
            },
            row = {
                listOf(
                    it.id,
                    it.userId,
                    it.productId,
                    it.rating,
                    it.status,
                    it.title.orEmpty(),
                    it.createdAt.orEmpty(),
                )
            },
        )
    }.describe { summary = "Admin: export reviews CSV" }
    adminWriteRateLimit {
        put("{id}/status") {
            val id = call.requirePathParameter("id")
            val body = call.receive<ReviewModerationRequest>()
            val result = reviewRatingRepo.moderateReview(id, body.status, body.reason)
            CatalogCache.invalidate()
            call.respondOk(result)
        }.describe {
            summary = "Admin: moderate a review"
            description = "Set ACTIVE, HIDDEN or FLAGGED"
            requestBody { jsonBody<ReviewModerationRequest>() }
            responses { ok<com.piashcse.model.response.ReviewRatingResponse>("Updated review") }
        }

        post("bulk/status") {
            val status = call.requireQueryParameter("status").parseEnum<ReviewStatus>("status")
            val body = call.receive<BulkIdsRequest>()
            val failed = body.ids.filter { runCatching { reviewRatingRepo.moderateReview(it, status, body.reason) }.isFailure }
            CatalogCache.invalidate()
            call.respondOk(BulkResultResponse.of(body.ids.size, failed))
        }.describe {
            summary = "Admin: bulk moderate reviews"
            requestBody { jsonBody<BulkIdsRequest>() }
        }
    }
}
