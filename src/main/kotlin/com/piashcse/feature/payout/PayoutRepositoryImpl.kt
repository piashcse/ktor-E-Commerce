package com.piashcse.feature.payout

import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.SellerPayoutTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.event.EventBus
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.retryQuery
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

class PayoutRepositoryImpl : PayoutRepository {
    private fun SellerPayoutDAO.toResponse() =
        PayoutResponse(
            id = id.value,
            sellerId = sellerId.value,
            orderId = orderId.value,
            subTotal = subTotal.toPlainString(),
            commissionAmount = commissionAmount.toPlainString(),
            payoutAmount = payoutAmount.toPlainString(),
            status = status,
            createdAt = createdAt.toString(),
        )

    override suspend fun sellerPayouts(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ) = query {
        val seller =
            SellerDAO.find { SellerTable.userId eq sellerUserId }.firstOrNull()
                ?: return@query PaginatedResponse(
                    emptyList(),
                    PaginationMetadata(0, limit, offset),
                )
        SellerPayoutTable.selectAll().andWhere { SellerPayoutTable.sellerId eq seller.id }
            .also { it.orderBy(SellerPayoutTable.createdAt to SortOrder.DESC) }
            .toPaginatedResponse(limit, offset) { SellerPayoutDAO.wrapRow(it).toResponse() }
    }

    override suspend fun allPayouts(
        limit: Int,
        offset: Int,
        status: String?,
    ) = query {
        SellerPayoutTable.selectAll()
            .also { q ->
                status?.let { q.andWhere { SellerPayoutTable.status eq it.uppercase() } }
                q.orderBy(SellerPayoutTable.createdAt to SortOrder.DESC)
            }
            .toPaginatedResponse(limit, offset) { SellerPayoutDAO.wrapRow(it).toResponse() }
    }

    override suspend fun markPaid(
        payoutId: String,
        actorId: String?,
    ) = query {
        val payout = SellerPayoutDAO.findById(payoutId) ?: payoutId.throwNotFound("Payout")
        // Idempotent PENDING -> PAID transition: an already-PAID row returns as-is
        // without rewriting paidAt; any other non-pending state is rejected.
        if (payout.status == "PAID") return@query payout.toResponse()
        if (payout.status != "PENDING") {
            throw ValidationException("Payout cannot be marked as paid from status ${payout.status}")
        }
        if (payout.rejectedAt != null) {
            throw ValidationException("Rejected payout $payoutId cannot be marked as paid")
        }
        payout.status = "PAID"
        payout.paidAt = LocalDateTime.now(ZoneOffset.UTC)
        if (!actorId.isNullOrBlank()) {
            val actor = runCatching { UserDAO.findById(actorId) }.getOrNull()
            EventBus.publishAdminAction(
                Triple(actorId, actor?.email.orEmpty(), actor?.userType?.name ?: "ADMIN"),
                "PAYOUT_MARKED_PAID",
                "PAYOUT",
                payoutId,
            )
        }
        // TODO: seller-stats accrual lives in OrderRepository (order completion path) — do
        // NOT accrue here. Accruing pre-discount subTotal without a reversal on refund
        // would double-count on partial/full refunds.
        payout.toResponse()
    }

    override suspend fun requestPayout(
        sellerUserId: String,
        amount: BigDecimal,
        requestKey: String?,
    ): List<PayoutResponse> =
        retryQuery {
            val seller = requireSellerByUserId(sellerUserId)
            if (amount <= BigDecimal.ZERO) {
                throw ValidationException("Payout amount must be greater than 0")
            }
            // Retry-safe replay: a retried request with the same Idempotency-Key
            // returns the rows claimed by the first attempt instead of claiming more.
            if (requestKey != null) {
                val replay =
                    SellerPayoutDAO.find {
                        (SellerPayoutTable.sellerId eq seller.id) and
                            (SellerPayoutTable.payoutKey eq requestKey)
                    }.toList()
                if (replay.isNotEmpty()) return@retryQuery replay.map { it.toResponse() }
            }
            // Available balance = PENDING rows not yet requested, rejected, or paid.
            val available =
                SellerPayoutDAO.find {
                    (SellerPayoutTable.sellerId eq seller.id) and
                        (SellerPayoutTable.status eq "PENDING") and
                        (SellerPayoutTable.requestedAt.isNull()) and
                        (SellerPayoutTable.rejectedAt.isNull())
                }.orderBy(SellerPayoutTable.createdAt to SortOrder.ASC).toList()
            val balance = available.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }
            if (balance <= BigDecimal.ZERO) {
                throw ValidationException("No available payout balance to request")
            }
            if (amount > balance) {
                throw ValidationException("Requested amount exceeds available payout balance")
            }
            // Claim oldest rows first until the requested amount is covered, in one
            // transaction; the V17 partial unique index on payout_key turns a
            // concurrent double-submit into a 409 instead of a double claim.
            val now = LocalDateTime.now(ZoneOffset.UTC)
            var covered = BigDecimal.ZERO
            val claimed = mutableListOf<SellerPayoutDAO>()
            for (row in available) {
                if (covered >= amount) break
                row.requestedAt = now
                if (requestKey != null) row.payoutKey = requestKey
                claimed.add(row)
                covered = covered.add(row.payoutAmount)
            }
            claimed.map { it.toResponse() }
        }

    override suspend fun approvePayout(payoutId: String) =
        query {
            val payout = SellerPayoutDAO.findById(payoutId) ?: payoutId.throwNotFound("Payout")
            if (payout.approvedAt != null) return@query payout.toResponse()
            if (payout.status != "PENDING" || payout.rejectedAt != null) {
                throw ValidationException("Payout $payoutId cannot be approved from its current state")
            }
            if (payout.requestedAt == null) {
                throw ValidationException("Payout $payoutId has not been requested")
            }
            payout.approvedAt = LocalDateTime.now(ZoneOffset.UTC)
            payout.toResponse()
        }

    override suspend fun rejectPayout(payoutId: String) =
        query {
            val payout = SellerPayoutDAO.findById(payoutId) ?: payoutId.throwNotFound("Payout")
            if (payout.rejectedAt != null) return@query payout.toResponse()
            if (payout.status != "PENDING" || payout.approvedAt != null) {
                throw ValidationException("Payout $payoutId cannot be rejected from its current state")
            }
            if (payout.requestedAt == null) {
                throw ValidationException("Payout $payoutId has not been requested")
            }
            payout.rejectedAt = LocalDateTime.now(ZoneOffset.UTC)
            payout.toResponse()
        }
}
