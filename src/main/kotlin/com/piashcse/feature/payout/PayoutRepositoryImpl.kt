package com.piashcse.feature.payout

import com.piashcse.constants.KycStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PayoutStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderTable
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
import com.piashcse.utils.common.Money
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
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
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
        if (seller.kycStatus != KycStatus.VERIFIED) {
            throw ForbiddenException("Seller KYC must be verified before payouts")
        }
        return seller
    }

    override suspend fun availableBalance(sellerUserId: String): PayoutBalanceResponse =
        query {
            val seller = requireActiveSeller(sellerUserId)
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
            val byCurrency =
                availableRows
                    .groupBy { it.currency }
                    .mapValues { (_, rows) ->
                        rows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }.toPlainString()
                    }
            val primaryCurrency =
                byCurrency.keys.firstOrNull()
                    ?: pendingRows.firstOrNull()?.currency
                    ?: "USD"
            PayoutBalanceResponse(
                available = byCurrency[primaryCurrency] ?: "0.00",
                currency = primaryCurrency,
                pendingRequestAmount = pendingRows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }.toPlainString(),
                balances = byCurrency,
            )
        }

    override suspend fun statement(
        sellerUserId: String,
        from: String?,
        to: String?,
    ): SellerStatementResponse =
        query {
            val seller = requireActiveSeller(sellerUserId)
            val fromDate = from?.takeIf { it.isNotBlank() }?.let { java.time.LocalDate.parse(it) }
            val toDate = to?.takeIf { it.isNotBlank() }?.let { java.time.LocalDate.parse(it) }
            if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
                throw ValidationException("from must be before to")
            }
            val base = SellerPayoutTable.selectAll().andWhere { SellerPayoutTable.sellerId eq seller.id }
            fromDate?.let { base.andWhere { SellerPayoutTable.createdAt greaterEq it.atStartOfDay() } }
            toDate?.let { base.andWhere { SellerPayoutTable.createdAt lessEq it.atTime(java.time.LocalTime.MAX) } }
            val payouts =
                base.orderBy(SellerPayoutTable.createdAt to SortOrder.ASC)
                    .toList()
                    .map { SellerPayoutDAO.wrapRow(it) }

            val tax =
                OrderDAO.find {
                    (OrderTable.shopId eq seller.shopId) and (OrderTable.status neq OrderStatus.CANCELED)
                }.filter { order ->
                    (fromDate == null || !order.createdAt.isBefore(fromDate.atStartOfDay())) &&
                        (toDate == null || !order.createdAt.isAfter(toDate.atTime(java.time.LocalTime.MAX)))
                }.fold(BigDecimal.ZERO) { acc, order ->
                    acc.add(order.taxAmount)
                }

            fun sum(statuses: Set<String>) =
                payouts.filter { it.status in statuses }.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }

            SellerStatementResponse(
                from = fromDate?.toString(),
                to = toDate?.toString(),
                currency = payouts.firstOrNull()?.currency ?: "USD",
                grossSales = Money.str(payouts.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.subTotal) }),
                commission = Money.str(payouts.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.commissionAmount) }),
                netPayout = Money.str(payouts.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }),
                paid = Money.str(sum(setOf(PayoutStatus.PAID.name))),
                pending = Money.str(sum(setOf(PayoutStatus.PENDING.name, PayoutStatus.REQUESTED.name, PayoutStatus.APPROVED.name))),
                available = Money.str(sum(setOf(PayoutStatus.PENDING.name))),
                taxCollected = Money.str(tax),
                rowCount = payouts.size,
                rows =
                    payouts.map {
                        SellerStatementRow(
                            payoutId = it.id.value,
                            orderId = it.orderId.value,
                            subTotal = Money.str(it.subTotal),
                            commissionAmount = Money.str(it.commissionAmount),
                            payoutAmount = Money.str(it.payoutAmount),
                            currency = it.currency,
                            status = it.status,
                            createdAt = it.createdAt.toUtcString(),
                        )
                    },
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
        if (payout.status == PayoutStatus.PAID.name) return@query payout.toResponse()
        if (payout.status != PayoutStatus.APPROVED.name) {
            throw ValidationException("Payout must be approved before it can be paid (current: ${payout.status})")
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
        payout.toResponse()
    }

    override suspend fun requestPayout(
        sellerUserId: String,
        amount: BigDecimal,
        requestKey: String?,
        currency: String?,
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

            val currencies = available.map { it.currency }.distinct()
            if (currencies.size > 1) {
                throw ValidationException(
                    "Multiple currencies available (${currencies.joinToString()}); specify a currency",
                )
            }
            val currency = currencies.firstOrNull() ?: normalizedCurrency ?: "USD"

            val balance = available.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }
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
                row.status = PayoutStatus.REQUESTED.name
                row.currency = currency
                row.rejectedAt = null
                claimed.add(row)
                covered = covered.add(row.payoutAmount)
            }
            AppMetrics.payoutsRequested.increment()
            claimed.map { it.toResponse() }
        }

    override suspend fun approvePayout(payoutId: String) =
        query {
            val payout = SellerPayoutDAO.findById(payoutId) ?: payoutId.throwNotFound("Payout")
            if (payout.status == PayoutStatus.APPROVED.name) return@query payout.toResponse()
            if (payout.status != PayoutStatus.REQUESTED.name) {
                throw ValidationException("Payout $payoutId cannot be approved from status ${payout.status}")
            }
            payout.status = PayoutStatus.APPROVED.name
            payout.approvedAt = currentUtc()
            payout.payoutRequestId?.let { requestId ->
                PayoutRequestDAO.findById(requestId.value)?.status = PayoutStatus.APPROVED.name
            }
            payout.toResponse()
        }

    override suspend fun rejectPayout(payoutId: String) =
        query {
            val payout = SellerPayoutDAO.findById(payoutId) ?: payoutId.throwNotFound("Payout")
            if (payout.status == PayoutStatus.REJECTED.name) return@query payout.toResponse()
            if (payout.status !in listOf(PayoutStatus.REQUESTED.name, PayoutStatus.APPROVED.name)) {
                throw ValidationException("Payout $payoutId cannot be rejected from status ${payout.status}")
            }
            val requestId = payout.payoutRequestId
            payout.rejectedAt = currentUtc()
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
            payout.toResponse()
        }
}
