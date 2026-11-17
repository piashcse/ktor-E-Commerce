@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

@file:Suppress("MatchingDeclarationName")

package com.piashcse.feature.kyc
import com.piashcse.constants.KycStatus
import com.piashcse.model.request.BulkIdsRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondCsv
import com.piashcse.utils.extension.respondOk
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class KycRejectRequest(val reason: String)

fun Route.sellerKycRoutes() {
    val service: KycService by inject()
    get {
        call.respondOk(service.get(call.currentUserId))
    }.describe {
        summary = "Get my KYC status"
        responses { ok<SellerKycResponse>("Current KYC state") }
    }
    get("history") {
        call.respondOk(service.history(call.currentUserId))
    }.describe {
        summary = "Get my KYC submission history"
        responses { ok<List<KycHistoryResponse>>("KYC history") }
    }
    sellerWriteRateLimit {
        post {
            call.respondOk(service.submit(call.currentUserId, call.receive<SellerKycSubmitRequest>()))
        }.describe {
            summary = "Submit KYC"
            description = "Business/bank details and optional document URL; marks PENDING"
            requestBody { jsonBody<SellerKycSubmitRequest>() }
            responses { ok<SellerKycResponse>("KYC submitted") }
        }
    }
}

fun Route.adminKycRoutes() {
    val service: KycService by inject()
    get {
        val (limit, offset) = call.pageRequest()
        val status = call.request.queryParameters["status"]?.parseEnum<KycStatus>("kyc status")
        call.respondOk(service.list(status, limit, offset))
    }.describe {
        summary = "Admin: list KYC submissions"
        description = "Optional status filter"
    }
    get("{sellerId}/history") {
        call.respondOk(service.adminHistory(call.requirePathParameter("sellerId")))
    }.describe {
        summary = "Admin: KYC decision history"
        responses { ok<List<KycHistoryResponse>>("KYC history") }
    }
    get("export") {
        call.respondCsv(
            "kyc.csv",
            listOf("seller_id", "email", "business_name", "kyc_status", "submitted_at", "verified_at", "rejection_reason"),
            fetch = { limit, offset ->
                service.list(
                    call.request.queryParameters["status"]?.parseEnum<KycStatus>("kyc status"),
                    limit,
                    offset,
                ).data
            },
            row = {
                listOf(
                    it.sellerId,
                    it.email.orEmpty(),
                    it.businessName.orEmpty(),
                    it.kycStatus,
                    it.submittedAt.orEmpty(),
                    it.verifiedAt.orEmpty(),
                    it.rejectionReason.orEmpty(),
                )
            },
        )
    }.describe { summary = "Admin: export KYC submissions CSV" }
    adminWriteRateLimit {
        put("{sellerId}/verify") {
            val sellerId = call.requirePathParameter("sellerId")
            call.respondOk(service.verify(sellerId, call.currentUserId))
        }.describe { summary = "Admin: verify KYC" }
        put("{sellerId}/reject") {
            val sellerId = call.requirePathParameter("sellerId")
            call.respondOk(service.reject(sellerId, call.receive<KycRejectRequest>().reason, call.currentUserId))
        }.describe {
            summary = "Admin: reject KYC"
            requestBody { jsonBody<KycRejectRequest>() }
        }
        post("bulk/verify") {
            val body = call.receive<BulkIdsRequest>()
            call.respondOk(service.bulkVerify(body.ids, call.currentUserId))
        }.describe {
            summary = "Admin: bulk verify KYC"
            requestBody { jsonBody<BulkIdsRequest>() }
        }
        post("bulk/reject") {
            val body = call.receive<BulkIdsRequest>()
            val reason =
                body.reason?.takeIf {
                    it.isNotBlank()
                } ?: throw com.piashcse.utils.validator.ValidationException("Rejection reason is required")
            call.respondOk(service.bulkReject(body.ids, reason, call.currentUserId))
        }.describe {
            summary = "Admin: bulk reject KYC"
            requestBody { jsonBody<BulkIdsRequest>() }
        }
    }
}
