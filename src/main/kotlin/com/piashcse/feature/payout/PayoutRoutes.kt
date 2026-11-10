@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.payout

import com.piashcse.plugin.adminAuth
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.sellerAuth
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.idempotencyKey
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

fun Route.payoutSellerRoutes() {
    val repo: PayoutRepository by inject()
    sellerAuth {
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(repo.sellerPayouts(call.currentUserId, limit, offset))
        }.describe { summary = "Seller: list payouts" }
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
            val statement =
                repo.statement(
                    call.currentUserId,
                    call.request.queryParameters["from"],
                    call.request.queryParameters["to"],
                )
            val csv =
                buildString {
                    appendLine("payout_id,order_id,sub_total,commission,payout,currency,status,created_at")
                    statement.rows.forEach {
                        appendLine(
                            listOf(
                                it.payoutId,
                                it.orderId,
                                it.subTotal,
                                it.commissionAmount,
                                it.payoutAmount,
                                it.currency,
                                it.status,
                                it.createdAt,
                            ).joinToString(","),
                        )
                    }
                }
            call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"statement.csv\"")
            call.respondText(csv, ContentType.parse("text/csv"))
        }.describe { summary = "Seller: export statement CSV" }
        post("request") {
            val body = call.receive<PayoutRequest>()
            call.respondCreated(repo.requestPayout(call.currentUserId, body.amount, call.idempotencyKey()))
        }.describe {
            summary = "Seller: request payout"
            description = "Requires VERIFIED KYC; supports Idempotency-Key"
            requestBody { jsonBody<PayoutRequest>() }
        }
    }
}

fun Route.payoutAdminRoutes() {
    val repo: PayoutRepository by inject()
    adminAuth {
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(repo.allPayouts(limit, offset, call.request.queryParameters["status"]))
        }.describe { summary = "Admin: list payouts" }
        post("{id}/approve") {
            call.respondOk(repo.approvePayout(call.parameters["id"] ?: throw IllegalArgumentException("id required")))
        }.describe { summary = "Admin: approve payout" }
        post("{id}/reject") {
            call.respondOk(repo.rejectPayout(call.parameters["id"] ?: throw IllegalArgumentException("id required")))
        }.describe { summary = "Admin: reject payout" }
        post("{id}/pay") {
            call.respondOk(
                repo.markPaid(
                    call.parameters["id"] ?: throw IllegalArgumentException("id required"),
                    call.currentUserId,
                ),
            )
        }.describe { summary = "Admin: mark payout paid" }
        get("export") {
            val data = repo.allPayouts(10000, 0, call.request.queryParameters["status"])
            val csv =
                buildString {
                    appendLine("id,seller_id,order_id,sub_total,commission,payout,status,created_at")
                    data.data.forEach {
                        appendLine(
                            listOf(
                                it.id,
                                it.sellerId,
                                it.orderId,
                                it.subTotal,
                                it.commissionAmount,
                                it.payoutAmount,
                                it.status,
                                it.createdAt,
                            ).joinToString(","),
                        )
                    }
                }
            call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"payouts.csv\"")
            call.respondText(csv, ContentType.parse("text/csv"))
        }.describe { summary = "Admin: export payouts CSV" }
    }
}
