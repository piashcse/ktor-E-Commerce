@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.payout

import com.piashcse.constants.PayoutStatus
import com.piashcse.model.request.BulkIdsRequest
import com.piashcse.model.request.BulkResultResponse
import com.piashcse.plugin.adminAuth
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.sellerAuth
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.idempotencyKey
import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.queryLocalDate
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondCsv
import com.piashcse.utils.extension.respondOk
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.request.requirePathParameter
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

fun Route.payoutSellerRoutes() {
    val repo: PayoutRepository by inject()
    sellerAuth {
        get {
            val (limit, offset) = call.pageRequest()
            val status = call.request.queryParameters["status"]?.parseEnum<PayoutStatus>("payout status")
            call.respondOk(
                repo.sellerPayouts(
                    call.currentUserId,
                    limit,
                    offset,
                    status,
                    call.queryLocalDate("from"),
                    call.queryLocalDate("to"),
                ),
            )
        }.describe {
            summary = "Seller: list payouts"
            description = "Optional status/from/to filters"
        }
        get("requests") {
            val (limit, offset) = call.pageRequest()
            val status = call.request.queryParameters["status"]?.parseEnum<PayoutStatus>("payout status")
            call.respondOk(repo.sellerPayoutRequests(call.currentUserId, status, limit, offset))
        }.describe { summary = "Seller: list payout requests" }
        get("requests/{id}") {
            call.respondOk(repo.sellerPayoutRequestDetail(call.currentUserId, call.requirePathParameter("id")))
        }.describe {
            summary = "Seller: payout request detail"
            responses { ok<PayoutRequestDetailResponse>("Payout request and rows") }
        }
        get("balance") {
            call.respondOk(repo.availableBalance(call.currentUserId))
        }.describe { summary = "Seller: available balance" }
        get("statement") {
            call.respondOk(
                repo.statement(
                    call.currentUserId,
                    call.request.queryParameters["from"],
                    call.request.queryParameters["to"],
                ),
            )
        }.describe {
            summary = "Seller: payout statement"
            description = "Optional from/to ISO dates"
        }
        get("statement/export") {
            val rows =
                repo.statement(
                    call.currentUserId,
                    call.request.queryParameters["from"],
                    call.request.queryParameters["to"],
                ).rows
            call.respondCsv(
                "statement.csv",
                listOf("payout_id", "order_id", "sub_total", "commission", "payout", "currency", "status", "created_at"),
                fetch = { _, offset -> if (offset == 0) rows else emptyList() },
                row = {
                    listOf(
                        it.payoutId,
                        it.orderId,
                        it.subTotal,
                        it.commissionAmount,
                        it.payoutAmount,
                        it.currency,
                        it.status,
                        it.createdAt,
                    )
                },
            )
        }.describe { summary = "Seller: export statement CSV" }
        post("request") {
            val body = call.receive<PayoutRequest>()
            call.respondCreated(repo.requestPayout(call.currentUserId, body.amount, call.idempotencyKey(), body.currency))
        }.describe {
            summary = "Seller: request payout"
            description = "Requires VERIFIED KYC; supports Idempotency-Key"
            requestBody { jsonBody<PayoutRequest>() }
        }
    }
}

private fun Route.adminPayoutRequestRoutes() {
    val repo: PayoutRepository by inject()
    get("requests") {
        val (limit, offset) = call.pageRequest()
        val status = call.request.queryParameters["status"]?.parseEnum<PayoutStatus>("payout status")
        call.respondOk(repo.allPayoutRequests(status, call.request.queryParameters["seller"], limit, offset))
    }.describe { summary = "Admin: list payout requests" }
    get("requests/{id}") {
        call.respondOk(repo.adminPayoutRequestDetail(call.requirePathParameter("id")))
    }.describe {
        summary = "Admin: payout request detail"
        responses { ok<PayoutRequestDetailResponse>("Payout request and rows") }
    }
    post("requests/{id}/approve") {
        call.respondOk(repo.approvePayoutRequest(call.requirePathParameter("id")))
    }.describe { summary = "Admin: approve a payout request" }
    post("requests/{id}/reject") {
        call.respondOk(
            repo.rejectPayoutRequest(
                call.requirePathParameter("id"),
                call.receive<PayoutDecisionRequest>().reason,
            ),
        )
    }.describe {
        summary = "Admin: reject a payout request"
        requestBody { jsonBody<PayoutDecisionRequest>() }
    }
    post("requests/{id}/pay") {
        call.respondOk(repo.payPayoutRequest(call.requirePathParameter("id")))
    }.describe { summary = "Admin: mark a payout request paid" }
    post("requests/bulk/approve") {
        val body = call.receive<BulkIdsRequest>()
        val failed = body.ids.filter { runCatching { repo.approvePayoutRequest(it) }.isFailure }
        call.respondOk(BulkResultResponse.of(body.ids.size, failed))
    }.describe {
        summary = "Admin: bulk approve payout requests"
        requestBody { jsonBody<BulkIdsRequest>() }
    }
    post("requests/bulk/reject") {
        val body = call.receive<BulkIdsRequest>()
        val failed = body.ids.filter { runCatching { repo.rejectPayoutRequest(it, body.reason) }.isFailure }
        call.respondOk(BulkResultResponse.of(body.ids.size, failed))
    }.describe {
        summary = "Admin: bulk reject payout requests"
        requestBody { jsonBody<BulkIdsRequest>() }
    }
    post("requests/bulk/pay") {
        val body = call.receive<BulkIdsRequest>()
        val failed = body.ids.filter { runCatching { repo.payPayoutRequest(it) }.isFailure }
        call.respondOk(BulkResultResponse.of(body.ids.size, failed))
    }.describe {
        summary = "Admin: bulk mark payout requests paid"
        requestBody { jsonBody<BulkIdsRequest>() }
    }
}

fun Route.payoutAdminRoutes() {
    val repo: PayoutRepository by inject()
    adminAuth {
        get {
            val (limit, offset) = call.pageRequest()
            val status = call.request.queryParameters["status"]?.parseEnum<PayoutStatus>("payout status")
            call.respondOk(
                repo.allPayouts(
                    limit,
                    offset,
                    status,
                    call.request.queryParameters["seller"],
                    call.queryLocalDate("from"),
                    call.queryLocalDate("to"),
                ),
            )
        }.describe {
            summary = "Admin: list payouts"
            description = "Optional status, seller email and date filters"
        }
        adminPayoutRequestRoutes()
        post("{id}/approve") {
            call.respondOk(repo.approvePayout(call.requirePathParameter("id")))
        }.describe { summary = "Admin: approve payout row" }
        post("{id}/reject") {
            call.respondOk(
                repo.rejectPayout(
                    call.requirePathParameter("id"),
                    call.receive<PayoutDecisionRequest>().reason,
                ),
            )
        }.describe {
            summary = "Admin: reject payout row"
            requestBody { jsonBody<PayoutDecisionRequest>() }
        }
        post("{id}/pay") {
            call.respondOk(
                repo.markPaid(
                    call.requirePathParameter("id"),
                    call.currentUserId,
                ),
            )
        }.describe { summary = "Admin: mark payout row paid" }
        get("export") {
            call.respondCsv(
                "payouts.csv",
                listOf(
                    "id",
                    "seller_id",
                    "seller_email",
                    "order_id",
                    "order_number",
                    "sub_total",
                    "commission",
                    "payout",
                    "status",
                    "created_at",
                ),
                fetch = { limit, offset ->
                    repo.allPayouts(
                        limit,
                        offset,
                        call.request.queryParameters["status"]?.parseEnum<PayoutStatus>("payout status"),
                        call.request.queryParameters["seller"],
                        call.queryLocalDate("from"),
                        call.queryLocalDate("to"),
                    ).data
                },
                row = {
                    listOf(
                        it.id,
                        it.sellerId,
                        it.sellerEmail.orEmpty(),
                        it.orderId,
                        it.orderNumber.orEmpty(),
                        it.subTotal,
                        it.commissionAmount,
                        it.payoutAmount,
                        it.status,
                        it.createdAt,
                    )
                },
            )
        }.describe { summary = "Admin: export payouts CSV" }
    }
}
