package com.piashcse.feature.payout

import com.piashcse.constants.AppConstants
import com.piashcse.constants.PayoutStatus
import com.piashcse.database.entities.PayoutRequestDAO
import com.piashcse.database.entities.PayoutRequestTable
import com.piashcse.database.entities.SellerBalanceAdjustmentDAO
import com.piashcse.database.entities.SellerBalanceAdjustmentTable
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.SellerPayoutTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.EventBus
import com.piashcse.event.PayoutStatusChangedEvent
import com.piashcse.service.AppMetrics
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.retryQuery
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import java.math.BigDecimal

class PayoutRepositoryImpl : PayoutRepository {
    private fun outstandingDebtByCurrency(sellerId: String): Map<String, BigDecimal> =
        SellerBalanceAdjustmentDAO.find {
            (SellerBalanceAdjustmentTable.sellerId eq sellerId.entityID(SellerTable)) and
                SellerBalanceAdjustmentTable.settledAt.isNull()
        }.groupBy { it.currency }
            .mapValues { (_, rows) -> rows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.amount.abs()) } }

    /** Consume outstanding seller debt against available PENDING rows. */
    private fun applyDebtToRows(
        sellerId: String,
        rows: List<SellerPayoutDAO>,
    ) {
        val adjustments =
            SellerBalanceAdjustmentDAO.find {
                (SellerBalanceAdjustmentTable.sellerId eq sellerId.entityID(SellerTable)) and
                    SellerBalanceAdjustmentTable.settledAt.isNull()
            }.orderBy(SellerBalanceAdjustmentTable.createdAt to SortOrder.ASC).toList()
        adjustments.forEach { adjustment ->
            var remaining = adjustment.amount.abs()
            val candidates =
                rows.filter {
                    it.currency == adjustment.currency && it.payoutAmount > BigDecimal.ZERO
                }.iterator()
            while (remaining > BigDecimal.ZERO && candidates.hasNext()) {
                val row = candidates.next()
                val take = remaining.min(row.payoutAmount)
                row.payoutAmount = row.payoutAmount.subtract(take).max(BigDecimal.ZERO)
                if (row.payoutAmount <= BigDecimal.ZERO) row.status = PayoutStatus.REFUNDED.name
                remaining = remaining.subtract(take)
            }
            if (remaining <= BigDecimal.ZERO) {
                adjustment.settledAt = currentUtc()
            }
        }
    }

    override suspend fun availableBalance(sellerUserId: String): PayoutBalanceResponse =
        query {
            val seller = PayoutSupport.requireActiveSeller(sellerUserId)
            val availableRows =
                SellerPayoutDAO.find {
                    (SellerPayoutTable.sellerId eq seller.id) and
                        (SellerPayoutTable.status eq PayoutStatus.PENDING.name) and
                        (SellerPayoutTable.requestedAt.isNull())
                }.toList()
            val pendingRows =
                SellerPayoutDAO.find {
                    (SellerPayoutTable.sellerId eq seller.id) and
                        (SellerPayoutTable.status inList listOf(PayoutStatus.REQUESTED.name, PayoutStatus.APPROVED.name))
                }.toList()
            val debtByCurrency = outstandingDebtByCurrency(seller.id.value)
            val availableByCurrency =
                availableRows
                    .groupBy { it.currency }
                    .mapValues { (currency, rows) ->
                        rows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }
                            .subtract(debtByCurrency[currency] ?: BigDecimal.ZERO)
                            .max(BigDecimal.ZERO)
                    }
            val byCurrency = availableByCurrency.mapValues { it.value.toPlainString() }
            val pendingByCurrency =
                pendingRows
                    .groupBy { it.currency }
                    .mapValues { (_, rows) -> rows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) } }
            val primaryCurrency =
                byCurrency.keys.firstOrNull()
                    ?: pendingByCurrency.keys.firstOrNull()
                    ?: AppConstants.DEFAULT_CURRENCY
            PayoutBalanceResponse(
                available = byCurrency[primaryCurrency] ?: "0.00",
                currency = primaryCurrency,
                pendingRequestAmount = (pendingByCurrency[primaryCurrency] ?: BigDecimal.ZERO).toPlainString(),
                balances = byCurrency,
            )
        }

    override suspend fun statement(
        sellerUserId: String,
        from: String?,
        to: String?,
        currency: String?,
    ): SellerStatementResponse = PayoutQueries.statement(sellerUserId, from, to, currency)

    override suspend fun sellerPayouts(
        sellerUserId: String,
        limit: Int,
        offset: Int,
        status: PayoutStatus?,
        from: java.time.LocalDate?,
        to: java.time.LocalDate?,
    ) = PayoutQueries.sellerPayouts(sellerUserId, limit, offset, status, from, to)

    override suspend fun allPayouts(
        limit: Int,
        offset: Int,
        status: PayoutStatus?,
        sellerQuery: String?,
        from: java.time.LocalDate?,
        to: java.time.LocalDate?,
    ) = PayoutQueries.allPayouts(limit, offset, status, sellerQuery, from, to)

    override suspend fun sellerPayoutRequests(
        sellerUserId: String,
        status: PayoutStatus?,
        limit: Int,
        offset: Int,
    ) = PayoutQueries.sellerPayoutRequests(sellerUserId, status, limit, offset)

    override suspend fun allPayoutRequests(
        status: PayoutStatus?,
        sellerQuery: String?,
        limit: Int,
        offset: Int,
    ) = PayoutQueries.allPayoutRequests(status, sellerQuery, limit, offset)

    override suspend fun approvePayoutRequest(requestId: String): PayoutRequestResponse = PayoutSettlementService.approveRequest(requestId)

    override suspend fun rejectPayoutRequest(
        requestId: String,
        reason: String?,
    ): PayoutRequestResponse = PayoutSettlementService.rejectRequest(requestId, reason)

    override suspend fun payPayoutRequest(requestId: String): PayoutRequestResponse = PayoutSettlementService.payRequest(requestId)

    override suspend fun sellerPayoutRequestDetail(
        sellerUserId: String,
        requestId: String,
    ): PayoutRequestDetailResponse = PayoutQueries.sellerPayoutRequestDetail(sellerUserId, requestId)

    override suspend fun adminPayoutRequestDetail(requestId: String): PayoutRequestDetailResponse =
        PayoutQueries.adminPayoutRequestDetail(requestId)

    override suspend fun markPaid(
        payoutId: String,
        actorId: String?,
    ) = PayoutSettlementService.markPaid(payoutId, actorId)

    override suspend fun requestPayout(
        sellerUserId: String,
        amount: BigDecimal,
        requestKey: String?,
        currency: String?,
    ): List<PayoutResponse> {
        val rows =
            retryQuery {
                val seller = PayoutSupport.requireActiveSeller(sellerUserId)
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
                        }.map { it.toPayoutResponse() }
                    }
                }

                val normalizedCurrency = currency?.takeIf { it.isNotBlank() }?.uppercase()
                val available =
                    SellerPayoutDAO.find {
                        (SellerPayoutTable.sellerId eq seller.id) and
                            (SellerPayoutTable.status eq PayoutStatus.PENDING.name) and
                            (SellerPayoutTable.requestedAt.isNull())
                    }.forUpdate()
                        .orderBy(SellerPayoutTable.createdAt to SortOrder.ASC)
                        .toList()
                        .filter { normalizedCurrency == null || it.currency.equals(normalizedCurrency, ignoreCase = true) }
                applyDebtToRows(seller.id.value, available)
                val claimable = available.filter { it.payoutAmount > BigDecimal.ZERO }

                val currencies = claimable.map { it.currency }.distinct()
                if (currencies.size > 1) {
                    throw ValidationException(
                        "Multiple currencies available (${currencies.joinToString()}); specify a currency",
                    )
                }
                val currency = currencies.firstOrNull() ?: normalizedCurrency ?: AppConstants.DEFAULT_CURRENCY

                val balance = claimable.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }
                if (balance <= BigDecimal.ZERO) throw ValidationException("No available payout balance to request")
                if (amount > balance) throw ValidationException("Requested amount exceeds available payout balance")
                // Payout rows are order-level and cannot be split safely without a
                // separate request-line table. Require a full withdrawal so a request
                // can never pay out more than the seller asked for.
                if (amount.compareTo(balance) != 0) {
                    throw ValidationException(
                        "Payout requests must withdraw the full available balance (${balance.toPlainString()})",
                    )
                }

                val request =
                    try {
                        PayoutRequestDAO.new {
                            this.sellerId = seller.id
                            this.idempotencyKey = requestKey
                            this.amount = amount
                            this.currency = currency
                            this.status = PayoutStatus.REQUESTED.name
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
                                }.map { it.toPayoutResponse() }
                            }
                        }
                        throw e
                    }

                val now = currentUtc()
                var covered = BigDecimal.ZERO
                val claimed = mutableListOf<SellerPayoutDAO>()
                for (row in claimable) {
                    if (covered >= amount) break
                    row.requestedAt = now
                    row.payoutRequestId = request.id
                    row.status = PayoutStatus.REQUESTED.name
                    row.currency = currency
                    row.rejectedAt = null
                    claimed.add(row)
                    covered = covered.add(row.payoutAmount)
                }
                AppMetrics.payoutsRequested.increment()
                claimed.map { it.toPayoutResponse() }
            }
        rows.firstOrNull()?.let { first ->
            val sellerUserId = query { SellerDAO.findById(first.sellerId)?.userId?.value }
            if (sellerUserId != null) {
                EventBus.publish(
                    PayoutStatusChangedEvent(
                        payoutRequestId = first.payoutRequestId.orEmpty(),
                        sellerId = first.sellerId,
                        userId = sellerUserId,
                        status = PayoutStatus.REQUESTED.name,
                        amount = rows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount.toBigDecimal()) },
                        currency = first.currency,
                    ),
                )
            }
        }
        return rows
    }

    override suspend fun approvePayout(payoutId: String) = PayoutSettlementService.approve(payoutId)

    override suspend fun rejectPayout(
        payoutId: String,
        reason: String?,
    ) = PayoutSettlementService.reject(payoutId, reason)
}
