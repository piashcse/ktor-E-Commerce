package com.piashcse.feature.payout

import com.piashcse.constants.AppConstants
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PayoutStatus
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PayoutRequestDAO
import com.piashcse.database.entities.PayoutRequestTable
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.SellerPayoutTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.common.emptyPage
import com.piashcse.utils.extension.readQuery
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedList
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

/** Read-only payout statements, lists and request details. */
internal object PayoutQueries {
    suspend fun statement(
        sellerUserId: String,
        from: String?,
        to: String?,
        currency: String?,
    ): SellerStatementResponse =
        readQuery {
            val seller = PayoutSupport.requireActiveSeller(sellerUserId)
            val fromDate =
                from?.takeIf { it.isNotBlank() }?.let {
                    runCatching { java.time.LocalDate.parse(it) }
                        .getOrElse { throw ValidationException("Invalid from date: $it") }
                }
            val toDate =
                to?.takeIf { it.isNotBlank() }?.let {
                    runCatching { java.time.LocalDate.parse(it) }
                        .getOrElse { throw ValidationException("Invalid to date: $it") }
                }
            if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
                throw ValidationException("from must be before to")
            }
            val base = SellerPayoutTable.selectAll().andWhere { SellerPayoutTable.sellerId eq seller.id }
            fromDate?.let { base.andWhere { SellerPayoutTable.createdAt greaterEq it.atStartOfDay() } }
            toDate?.let { base.andWhere { SellerPayoutTable.createdAt lessEq it.atTime(java.time.LocalTime.MAX) } }
            val allPayouts =
                base.orderBy(SellerPayoutTable.createdAt to SortOrder.ASC)
                    .toList()
                    .map { SellerPayoutDAO.wrapRow(it) }
            val selectedCurrency =
                currency?.takeIf { it.isNotBlank() }?.uppercase()
                    ?: allPayouts.firstOrNull()?.currency
                    ?: AppConstants.DEFAULT_CURRENCY
            val payouts = allPayouts.filter { it.currency.equals(selectedCurrency, ignoreCase = true) }

            val taxQuery =
                OrderTable.select(OrderTable.taxAmount).apply {
                    andWhere {
                        (OrderTable.shopId eq seller.shopId) and
                            (OrderTable.status neq OrderStatus.CANCELED) and
                            (OrderTable.currency eq selectedCurrency)
                    }
                    fromDate?.let { date -> andWhere { OrderTable.createdAt greaterEq date.atStartOfDay() } }
                    toDate?.let { date -> andWhere { OrderTable.createdAt lessEq date.atTime(java.time.LocalTime.MAX) } }
                }
            val tax =
                taxQuery.fold(BigDecimal.ZERO) { acc, row ->
                    acc.add(row[OrderTable.taxAmount])
                }

            fun sum(statuses: Set<String>) =
                payouts.filter { it.status in statuses }.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }

            SellerStatementResponse(
                from = fromDate?.toString(),
                to = toDate?.toString(),
                currency = selectedCurrency,
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

    suspend fun sellerPayouts(
        sellerUserId: String,
        limit: Int,
        offset: Int,
        status: PayoutStatus?,
        from: java.time.LocalDate?,
        to: java.time.LocalDate?,
    ) = readQuery {
        val sellerIds = PayoutSupport.sellerIdsFor(sellerUserId)
        if (sellerIds.isEmpty()) {
            return@readQuery emptyPage(limit, offset)
        }
        val page =
            SellerPayoutTable.selectAll().andWhere { SellerPayoutTable.sellerId inList sellerIds }
                .also { q ->
                    status?.let { q.andWhere { SellerPayoutTable.status eq it.name } }
                    from?.let { date -> q.andWhere { SellerPayoutTable.createdAt greaterEq date.atStartOfDay() } }
                    to?.let { date -> q.andWhere { SellerPayoutTable.createdAt lessEq date.atTime(java.time.LocalTime.MAX) } }
                    q.orderBy(SellerPayoutTable.createdAt to SortOrder.DESC)
                }.toPaginatedResponse(limit, offset) { SellerPayoutDAO.wrapRow(it).toPayoutResponse() }
        enrichPayouts(page, includeSellerEmail = false)
    }

    suspend fun allPayouts(
        limit: Int,
        offset: Int,
        status: PayoutStatus?,
        sellerQuery: String?,
        from: java.time.LocalDate?,
        to: java.time.LocalDate?,
    ) = readQuery {
        val sellerIds =
            sellerQuery?.takeIf { it.isNotBlank() }?.let { query ->
                val userIds =
                    UserDAO.find { UserTable.email like "%${query.trim().lowercase()}%" }
                        .map { it.id }
                if (userIds.isEmpty()) {
                    emptyList()
                } else {
                    SellerDAO.find { SellerTable.userId inList userIds }.map { it.id }
                }
            }
        if (sellerIds != null && sellerIds.isEmpty()) {
            return@readQuery emptyPage(limit, offset)
        }
        val page =
            SellerPayoutTable.selectAll()
                .also { q ->
                    status?.let { q.andWhere { SellerPayoutTable.status eq it.name } }
                    sellerIds?.let { q.andWhere { SellerPayoutTable.sellerId inList it } }
                    from?.let { date -> q.andWhere { SellerPayoutTable.createdAt greaterEq date.atStartOfDay() } }
                    to?.let { date -> q.andWhere { SellerPayoutTable.createdAt lessEq date.atTime(java.time.LocalTime.MAX) } }
                    q.orderBy(SellerPayoutTable.createdAt to SortOrder.DESC)
                }.toPaginatedResponse(limit, offset) { SellerPayoutDAO.wrapRow(it).toPayoutResponse() }
        enrichPayouts(page, includeSellerEmail = true)
    }

    suspend fun sellerPayoutRequests(
        sellerUserId: String,
        status: PayoutStatus?,
        limit: Int,
        offset: Int,
    ) = readQuery {
        val sellerIds = PayoutSupport.sellerIdsFor(sellerUserId)
        if (sellerIds.isEmpty()) {
            return@readQuery emptyPage(limit, offset)
        }
        val (total, rows) =
            PayoutRequestTable.selectAll()
                .andWhere { PayoutRequestTable.sellerId inList sellerIds }
                .also { q ->
                    status?.let { q.andWhere { PayoutRequestTable.status eq it.name } }
                    q.orderBy(PayoutRequestTable.createdAt to SortOrder.DESC)
                }.toPaginatedList(limit, offset) { PayoutRequestDAO.wrapRow(it) }
        PaginatedResponse(payoutRequestResponses(rows, includeSellerEmail = false), PaginationMetadata(total, limit, offset))
    }

    suspend fun allPayoutRequests(
        status: PayoutStatus?,
        sellerQuery: String?,
        limit: Int,
        offset: Int,
    ) = readQuery {
        val sellerIds =
            sellerQuery?.takeIf { it.isNotBlank() }?.let { query ->
                val userIds =
                    UserDAO.find { UserTable.email like "%${query.trim().lowercase()}%" }
                        .map { it.id }
                if (userIds.isEmpty()) emptyList() else SellerDAO.find { SellerTable.userId inList userIds }.map { it.id }
            }
        if (sellerIds != null && sellerIds.isEmpty()) {
            return@readQuery emptyPage(limit, offset)
        }
        val (total, rows) =
            PayoutRequestTable.selectAll()
                .also { q ->
                    status?.let { q.andWhere { PayoutRequestTable.status eq it.name } }
                    sellerIds?.let { q.andWhere { PayoutRequestTable.sellerId inList it } }
                    q.orderBy(PayoutRequestTable.createdAt to SortOrder.DESC)
                }.toPaginatedList(limit, offset) { PayoutRequestDAO.wrapRow(it) }
        PaginatedResponse(payoutRequestResponses(rows, includeSellerEmail = true), PaginationMetadata(total, limit, offset))
    }

    suspend fun sellerPayoutRequestDetail(
        sellerUserId: String,
        requestId: String,
    ): PayoutRequestDetailResponse =
        readQuery {
            val request = PayoutRequestDAO.findById(requestId) ?: requestId.throwNotFound("Payout request")
            if (request.sellerId !in PayoutSupport.sellerIdsFor(sellerUserId)) {
                throw ForbiddenException("Payout request belongs to another seller")
            }
            buildRequestDetail(request)
        }

    suspend fun adminPayoutRequestDetail(requestId: String): PayoutRequestDetailResponse =
        readQuery {
            val request = PayoutRequestDAO.findById(requestId) ?: requestId.throwNotFound("Payout request")
            buildRequestDetail(request)
        }

    private fun buildRequestDetail(request: PayoutRequestDAO): PayoutRequestDetailResponse {
        val rows = SellerPayoutDAO.find { SellerPayoutTable.payoutRequestId eq request.id }.toList()
        val page =
            PaginatedResponse(
                rows.map { it.toPayoutResponse() },
                PaginationMetadata(rows.size.toLong(), rows.size.coerceAtLeast(1), 0),
            )
        val enriched = enrichPayouts(page, includeSellerEmail = true)
        return PayoutRequestDetailResponse(
            payoutRequestResponses(listOf(request), includeSellerEmail = true).single(),
            enriched.data,
        )
    }
}
