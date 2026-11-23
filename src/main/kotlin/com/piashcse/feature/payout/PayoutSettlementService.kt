package com.piashcse.feature.payout

import com.piashcse.constants.PayoutStatus
import com.piashcse.database.entities.PayoutRequestDAO
import com.piashcse.database.entities.PayoutRequestTable
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.SellerPayoutTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.EventBus
import com.piashcse.event.PayoutStatusChangedEvent
import com.piashcse.service.AppMetrics
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ConflictException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq

/** Payout request transitions and individual payout settlement. */
internal object PayoutSettlementService {
    suspend fun transitionRequest(
        requestId: String,
        target: PayoutStatus,
        reason: String? = null,
    ): PayoutRequestResponse {
        val response =
            query {
                val request =
                    PayoutRequestDAO.find { PayoutRequestTable.id eq requestId.entityID(PayoutRequestTable) }
                        .forUpdate()
                        .firstOrNull() ?: requestId.throwNotFound("Payout request")
                val current = request.status
                if (current == target.name) return@query payoutRequestResponses(listOf(request), includeSellerEmail = true).single()
                val allowed =
                    when (current) {
                        PayoutStatus.REQUESTED.name -> setOf(PayoutStatus.APPROVED.name, PayoutStatus.REJECTED.name)
                        PayoutStatus.APPROVED.name -> setOf(PayoutStatus.PAID.name, PayoutStatus.REJECTED.name)
                        else -> emptySet()
                    }
                if (target.name !in allowed) {
                    throw ConflictException("Payout request $requestId cannot change from $current to ${target.name}")
                }
                val now = currentUtc()
                val rows =
                    SellerPayoutDAO.find { SellerPayoutTable.payoutRequestId eq request.id }
                        .forUpdate()
                        .toList()
                // A request can be APPROVED while some of its rows have already
                // been paid individually. Rejecting the request must never reset
                // those rows to PENDING, or the same money becomes withdrawable
                // twice.
                if (target == PayoutStatus.REJECTED &&
                    rows.any { it.status == PayoutStatus.PAID.name || it.status == PayoutStatus.REFUNDED.name }
                ) {
                    throw ConflictException("Payout request contains already-paid rows and cannot be rejected")
                }
                request.status = target.name
                request.rejectionReason = if (target == PayoutStatus.REJECTED) reason?.takeIf { it.isNotBlank() } else null
                rows.forEach { row ->
                    when (target) {
                        PayoutStatus.APPROVED -> {
                            row.status = PayoutStatus.APPROVED.name
                            row.approvedAt = now
                        }
                        PayoutStatus.PAID -> {
                            row.status = PayoutStatus.PAID.name
                            row.paidAt = now
                        }
                        PayoutStatus.REJECTED -> {
                            // Release the rows back to the seller's available balance.
                            row.rejectionReason = reason?.takeIf { it.isNotBlank() }
                            row.rejectedAt = now
                            row.status = PayoutStatus.PENDING.name
                            row.requestedAt = null
                            row.approvedAt = null
                            row.payoutRequestId = null
                        }
                        else -> Unit
                    }
                }
                if (target == PayoutStatus.PAID) AppMetrics.payoutsPaid.increment()
                payoutRequestResponses(listOf(request), includeSellerEmail = true).single()
            }
        publishPayoutEvent(response)
        return response
    }

    private fun publishPayoutEvent(response: PayoutRequestResponse) {
        val sellerUserId = SellerDAO.findById(response.sellerId)?.userId?.value ?: return
        EventBus.publish(
            PayoutStatusChangedEvent(
                payoutRequestId = response.id,
                sellerId = response.sellerId,
                userId = sellerUserId,
                status = response.status,
                amount = response.amount.toBigDecimal(),
                currency = response.currency,
                reason = response.rejectionReason,
            ),
        )
    }

    suspend fun approveRequest(requestId: String): PayoutRequestResponse = transitionRequest(requestId, PayoutStatus.APPROVED)

    suspend fun rejectRequest(
        requestId: String,
        reason: String?,
    ): PayoutRequestResponse = transitionRequest(requestId, PayoutStatus.REJECTED, reason)

    suspend fun payRequest(requestId: String): PayoutRequestResponse = transitionRequest(requestId, PayoutStatus.PAID)

    suspend fun markPaid(
        payoutId: String,
        actorId: String?,
    ) = query {
        val payout =
            SellerPayoutDAO.find { SellerPayoutTable.id eq payoutId.entityID(SellerPayoutTable) }
                .forUpdate()
                .firstOrNull() ?: payoutId.throwNotFound("Payout")
        if (payout.status == PayoutStatus.PAID.name) return@query payout.toPayoutResponse()
        if (payout.status != PayoutStatus.APPROVED.name) {
            throw ConflictException("Payout must be approved before it can be paid (current: ${payout.status})")
        }
        payout.status = PayoutStatus.PAID.name
        payout.paidAt = currentUtc()
        AppMetrics.payoutsPaid.increment()
        payout.payoutRequestId?.let { requestId ->
            val remaining =
                SellerPayoutDAO.find {
                    (SellerPayoutTable.payoutRequestId eq requestId) and (SellerPayoutTable.status neq PayoutStatus.PAID.name)
                }.count()
            if (remaining == 0L) {
                PayoutRequestDAO.findById(requestId.value)?.status = PayoutStatus.PAID.name
            }
        }
        if (!actorId.isNullOrBlank()) {
            val actor = runCatching { UserDAO.findById(actorId) }.getOrNull()
            EventBus.publishAdminAction(
                Triple(actorId, actor?.email.orEmpty(), actor?.userType?.name ?: "ADMIN"),
                "PAYOUT_MARKED_PAID",
                "PAYOUT",
                payoutId,
            )
        }
        payout.toPayoutResponse()
    }

    suspend fun approve(payoutId: String) =
        query {
            val payout =
                SellerPayoutDAO.find { SellerPayoutTable.id eq payoutId.entityID(SellerPayoutTable) }
                    .forUpdate()
                    .firstOrNull() ?: payoutId.throwNotFound("Payout")
            if (payout.status == PayoutStatus.APPROVED.name) return@query payout.toPayoutResponse()
            if (payout.status != PayoutStatus.REQUESTED.name) {
                throw ConflictException("Payout $payoutId cannot be approved from status ${payout.status}")
            }
            payout.status = PayoutStatus.APPROVED.name
            payout.approvedAt = currentUtc()
            payout.payoutRequestId?.let { requestId ->
                PayoutRequestDAO.findById(requestId.value)?.status = PayoutStatus.APPROVED.name
            }
            payout.toPayoutResponse()
        }

    suspend fun reject(
        payoutId: String,
        reason: String?,
    ) = query {
        val payout =
            SellerPayoutDAO.find { SellerPayoutTable.id eq payoutId.entityID(SellerPayoutTable) }
                .forUpdate()
                .firstOrNull() ?: payoutId.throwNotFound("Payout")
        if (payout.status == PayoutStatus.REJECTED.name) return@query payout.toPayoutResponse()
        if (payout.status !in listOf(PayoutStatus.REQUESTED.name, PayoutStatus.APPROVED.name)) {
            throw ConflictException("Payout $payoutId cannot be rejected from status ${payout.status}")
        }
        val requestId = payout.payoutRequestId
        payout.rejectedAt = currentUtc()
        payout.rejectionReason = reason?.takeIf { it.isNotBlank() }
        payout.status = PayoutStatus.PENDING.name
        payout.requestedAt = null
        payout.approvedAt = null
        payout.payoutRequestId = null
        if (requestId != null) {
            val remaining =
                SellerPayoutDAO.find {
                    (SellerPayoutTable.payoutRequestId eq requestId) and
                        (SellerPayoutTable.id neq payout.id)
                }.count()
            if (remaining == 0L) PayoutRequestDAO.findById(requestId.value)?.status = PayoutStatus.REJECTED.name
        }
        payout.toPayoutResponse()
    }
}
