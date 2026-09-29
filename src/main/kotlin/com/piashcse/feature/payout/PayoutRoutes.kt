package com.piashcse.feature.payout

import com.piashcse.plugin.adminAuth
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
import org.koin.ktor.ext.inject

fun Route.payoutSellerRoutes() {
    val repo: PayoutRepository by inject()
    sellerAuth {
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(repo.sellerPayouts(call.currentUserId, limit, offset))
        }
        post("request") {
            val body = call.receive<PayoutRequest>()
            call.respondCreated(repo.requestPayout(call.currentUserId, body.amount, call.idempotencyKey()))
        }
    }
}

fun Route.payoutAdminRoutes() {
    val repo: PayoutRepository by inject()
    adminAuth {
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(repo.allPayouts(limit, offset, call.request.queryParameters["status"]))
        }
        post("{id}/approve") {
            call.respondOk(repo.approvePayout(call.parameters["id"] ?: throw IllegalArgumentException("id required")))
        }
        post("{id}/reject") {
            call.respondOk(repo.rejectPayout(call.parameters["id"] ?: throw IllegalArgumentException("id required")))
        }
        post("{id}/pay") {
            call.respondOk(
                repo.markPaid(
                    call.parameters["id"] ?: throw IllegalArgumentException("id required"),
                    call.currentUserId,
                ),
            )
        }
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
        }
    }
}
