@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.seller

import com.piashcse.constants.DisputeStatus
import com.piashcse.constants.ReviewStatus
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.standardErrors
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Public seller review listings. */
fun Route.sellerPublicRoutes() {
    val service: SellerToolsService by inject()
    get("{id}/reviews") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.reviewsForSeller(call.requirePathParameter("id"), limit, offset))
    }.describe { summary = "List active reviews for a seller" }
}

/** Customer creates a seller review after delivery. */
fun Route.sellerReviewRoutes() {
    val service: SellerToolsService by inject()
    customerOnlyAuth {
        post {
            call.respondCreated(service.createReview(call.currentUserId, call.receive<SellerReviewRequest>()))
        }.describe {
            summary = "Review a seller after delivery"
            requestBody { jsonBody<SellerReviewRequest>() }
            responses {
                created<SellerReviewResponse>("Review created")
                standardErrors()
            }
        }
    }
}

/** Seller reads reviews received by their shop. */
fun Route.sellerOwnReviewRoutes() {
    val service: SellerToolsService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.reviewsForSellerUser(call.currentUserId, limit, offset))
    }.describe { summary = "Seller: list reviews received" }
}

/** Admin moderation of seller reviews. */
fun Route.sellerReviewAdminRoutes() {
    val service: SellerToolsService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        val status = call.request.queryParameters["status"]?.parseEnum<ReviewStatus>("review status")
        call.respondOk(service.adminReviews(status, limit, offset))
    }.describe { summary = "Admin: list seller reviews" }

    adminWriteRateLimit {
        put("{id}/status") {
            val id = call.requirePathParameter("id")
            call.respondOk(service.updateReviewStatus(id, call.receive<SellerReviewStatusRequest>().status))
        }.describe {
            summary = "Admin: moderate a seller review"
            requestBody { jsonBody<SellerReviewStatusRequest>() }
            responses { ok<SellerReviewResponse>("Review updated") }
        }
    }
}

/** Buyer/seller dispute creation and history. */
fun Route.disputeRoutes() {
    val service: SellerToolsService by inject()
    requireRole {
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(service.myDisputes(call.currentUserId, limit, offset))
        }.describe { summary = "List my disputes" }

        post {
            call.respondCreated(service.createDispute(call.currentUserId, call.receive<DisputeRequest>()))
        }.describe {
            summary = "Open a dispute"
            requestBody { jsonBody<DisputeRequest>() }
            responses {
                created<DisputeResponse>("Dispute created")
                standardErrors()
            }
        }
    }
}

/** Admin dispute resolution. */
fun Route.disputeAdminRoutes() {
    val service: SellerToolsService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        val status = call.request.queryParameters["status"]?.parseEnum<DisputeStatus>("dispute status")
        call.respondOk(service.adminDisputes(status, limit, offset))
    }.describe { summary = "Admin: list disputes" }

    adminWriteRateLimit {
        put("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(service.updateDispute(id, call.currentUserId, call.receive<DisputeUpdateRequest>()))
        }.describe {
            summary = "Admin: resolve/update a dispute"
            requestBody { jsonBody<DisputeUpdateRequest>() }
            responses {
                ok<DisputeResponse>("Dispute updated")
                standardErrors()
            }
        }
    }
}

/** Admin commission-rule management. */
fun Route.commissionAdminRoutes() {
    val service: CommissionService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.list(limit, offset))
    }.describe { summary = "List commission rules" }

    adminWriteRateLimit {
        post {
            call.respondCreated(service.create(call.receive<CommissionRuleRequest>()))
        }.describe {
            summary = "Create a commission rule"
            requestBody { jsonBody<CommissionRuleRequest>() }
            responses { created<CommissionRuleResponse>("Commission rule created") }
        }

        put("{id}") {
            call.respondOk(
                service.update(call.requirePathParameter("id"), call.receive<CommissionRuleRequest>()),
            )
        }.describe {
            summary = "Update a commission rule"
            requestBody { jsonBody<CommissionRuleRequest>() }
            responses { ok<CommissionRuleResponse>("Commission rule updated") }
        }

        delete("{id}") {
            val id = call.requirePathParameter("id")
            if (!service.delete(id)) throw NotFoundException("Commission rule not found")
            call.respondOk(MessageResponse("Commission rule deleted"))
        }.describe { summary = "Delete a commission rule" }
    }
}

/** Seller's effective commission rate. */
fun Route.commissionSellerRoutes() {
    val service: CommissionService by inject()
    get {
        call.respondOk(
            service.effectiveRate(
                call.currentUserId,
                call.request.queryParameters["categoryId"],
            ),
        )
    }.describe { summary = "Seller: get my effective commission rate" }
}
