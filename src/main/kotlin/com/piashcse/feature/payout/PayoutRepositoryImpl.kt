package com.piashcse.feature.payout

import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.PayoutRequestDAO
import com.piashcse.database.entities.PayoutRequestTable
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.SellerPayoutTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.EventBus
import com.piashcse.service.AppMetrics
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.retryQuery
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

class PayoutRepositoryImpl : PayoutRepository {
    private fun SellerPayoutDAO.toResponse() =
        PayoutResponse(
            id = id.value,
            sellerId = sellerId.value,
            orderId = orderId.value,
            subTotal = subTotal.toPlainString(),
            commissionAmount = commissionAmount.toPlainString(),
            payoutAmount = payoutAmount.toPlainString(),
            currency = currency,
            status = status,
            payoutRequestId = payoutRequestId?.value,
            requestedAt = requestedAt?.toUtcString(),
            approvedAt = approvedAt?.toUtcString(),
            rejectedAt = rejectedAt?.toUtcString(),
            paidAt = paidAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
        )

    private fun sellerIdsFor(sellerUserId: String): List<EntityID<String>> =
        SellerDAO.find { SellerTable.userId eq sellerUserId }.map { it.id }

    private fun requireActiveSeller(sellerUserId: String): SellerDAO {
        val seller = requireSellerByUserId(sellerUserId)
        val user = UserDAO.findById(sellerUserId)
        if (user?.isActive != true) throw ForbiddenException("Seller account is inactive")
        if (seller.status != ShopStatus.APPROVED) throw ForbiddenException("Seller is not approved for payouts")
        return seller
    }

    override suspend fun availableBalance(sellerUserId: String): PayoutBalanceResponse =
        query {
            val seller = requireActiveSeller(sellerUserId)
            val availableRows =
                SellerPayoutDAO.find {
                    (SellerPayoutTable.sellerId eq seller.id) and
                        (SellerPayoutTable.status eq "PENDING") and
                        (SellerPayoutTable.requestedAt.isNull()) and
                        (SellerPayoutTable.rejectedAt.isNull())
                }.toList()
            val pendingRows =
                SellerPayoutDAO.find {
                    (SellerPayoutTable.sellerId eq seller.id) and
                        (SellerPayoutTable.status inList listOf("REQUESTED", "APPROVED"))
                }.toList()
            PayoutBalanceResponse(
                available = availableRows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }.toPlainString(),
                currency = availableRows.firstOrNull()?.currency ?: pendingRows.firstOrNull()?.currency ?: "USD",
                pendingRequestAmount = pendingRows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }.toPlainString(),
            )
        }

    override suspend fun sellerPayouts(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ) = query {
        val sellerIds = sellerIdsFor(sellerUserId)
        if (sellerIds.isEmpty()) {
            return@query PaginatedResponse(emptyList(), PaginationMetadata(0, limit, offset))
        }
        SellerPayoutTable.selectAll().andWhere { SellerPayoutTable.sellerId inList sellerIds }
            .orderBy(SellerPayoutTable.createdAt to SortOrder.DESC)
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
        if (payout.status == "PAID") return@query payout.toResponse()
        if (payout.status != "APPROVED") {
            throw ValidationException("Payout must be approved before it can be paid (current: ${payout.status})")
        }
        payout.status = "PAID"
        payout.paidAt = currentUtc()
        AppMetrics.payoutsPaid.increment()
        payout.payoutRequestId?.let { requestId ->
            val remaining =
                SellerPayoutDAO.find {
                    (SellerPayoutTable.payoutRequestId eq requestId) and (SellerPayoutTable.status neq "PAID")
                }.count()
            if (remaining == 0L) {
                PayoutRequestDAO.findById(requestId.value)?.status = "PAID"
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
        payout.toResponse()
    }

    override suspend fun requestPayout(
        sellerUserId: String,
        amount: BigDecimal,
        requestKey: String?,
    ): List<PayoutResponse> =
        retryQuery {
            val seller = requireActiveSeller(sellerUserId)
            if (amount <= BigDecimal.ZERO) {
                throw ValidationException("Payout amount must be greater than 0")
            }
            if (requestKey != null && requestKey.length > 100) {
                throw ValidationException("Idempotency-Key must be at most 100 characters")
            }

            // Retry-safe replay: return the rows bound to the existing request.
            if (requestKey != null) {
                val existing =
                    PayoutRequestDAO.find {
                        (PayoutRequestTable.sellerId eq seller.id) and
                            (PayoutRequestTable.idempotencyKey eq requestKey)
                    }.firstOrNull()
                if (existing != null) {
                    return@retryQuery SellerPayoutDAO.find {
                        SellerPayoutTable.payoutRequestId eq existing.id
                    }.map { it.toResponse() }
                }
            }

            val available =
                SellerPayoutDAO.find {
                    (SellerPayoutTable.sellerId eq seller.id) and
                        (SellerPayoutTable.status eq "PENDING") and
                        (SellerPayoutTable.requestedAt.isNull()) and
                        (SellerPayoutTable.rejectedAt.isNull())
                }.forUpdate().orderBy(SellerPayoutTable.createdAt to SortOrder.ASC).toList()

            val currencies = available.map { it.currency }.distinct()
            if (currencies.size > 1) {
                throw ValidationException("Cannot combine payouts in different currencies: ${currencies.joinToString()}")
            }
            val currency = currencies.firstOrNull() ?: "USD"

            val balance = available.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }
            if (balance <= BigDecimal.ZERO) throw ValidationException("No available payout balance to request")
            if (amount > balance) throw ValidationException("Requested amount exceeds available payout balance")

            val request =
                try {
                    PayoutRequestDAO.new {
                        this.sellerId = seller.id
                        this.idempotencyKey = requestKey
                        this.amount = amount
                        this.currency = currency
                        this.status = "REQUESTED"
                    }
                } catch (e: Exception) {
                    // Unique (seller, key) race: return the winner's rows.
                    if (requestKey != null) {
                        val winner =
                            PayoutRequestDAO.find {
                                (PayoutRequestTable.sellerId eq seller.id) and
                                    (PayoutRequestTable.idempotencyKey eq requestKey)
                            }.firstOrNull()
                        if (winner != null) {
                            return@retryQuery SellerPayoutDAO.find {
                                SellerPayoutTable.payoutRequestId eq winner.id
                            }.map { it.toResponse() }
                        }
                    }
                    throw e
                }

            val now = currentUtc()
            var covered = BigDecimal.ZERO
            val claimed = mutableListOf<SellerPayoutDAO>()
            for (row in available) {
                if (covered >= amount) break
                row.requestedAt = now
                row.payoutRequestId = request.id
                row.status = "REQUESTED"
                row.currency = currency
                claimed.add(row)
                covered = covered.add(row.payoutAmount)
            }
            AppMetrics.payoutsRequested.increment()
            claimed.map { it.toResponse() }
        }

    override suspend fun approvePayout(payoutId: String) =
        query {
            val payout = SellerPayoutDAO.findById(payoutId) ?: payoutId.throwNotFound("Payout")
            if (payout.status == "APPROVED") return@query payout.toResponse()
            if (payout.status != "REQUESTED") {
                throw ValidationException("Payout $payoutId cannot be approved from status ${payout.status}")
            }
            payout.status = "APPROVED"
            payout.approvedAt = currentUtc()
            payout.payoutRequestId?.let { requestId ->
                PayoutRequestDAO.findById(requestId.value)?.status = "APPROVED"
            }
            payout.toResponse()
        }

    override suspend fun rejectPayout(payoutId: String) =
        query {
            val payout = SellerPayoutDAO.findById(payoutId) ?: payoutId.throwNotFound("Payout")
            if (payout.status == "REJECTED") return@query payout.toResponse()
            if (payout.status !in listOf("REQUESTED", "APPROVED")) {
                throw ValidationException("Payout $payoutId cannot be rejected from status ${payout.status}")
            }
            val requestId = payout.payoutRequestId
            payout.rejectedAt = currentUtc()
            payout.status = "PENDING"
            payout.requestedAt = null
            payout.approvedAt = null
            payout.payoutRequestId = null
            if (requestId != null) {
                val remaining =
                    SellerPayoutDAO.find {
                        (SellerPayoutTable.payoutRequestId eq requestId) and
                            (SellerPayoutTable.id neq payout.id)
                    }.count()
                if (remaining == 0L) PayoutRequestDAO.findById(requestId.value)?.status = "REJECTED"
            }
            payout.toResponse()
        }
}
