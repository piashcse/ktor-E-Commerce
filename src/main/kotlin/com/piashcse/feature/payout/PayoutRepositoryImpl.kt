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
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.EventBus
import com.piashcse.event.PayoutStatusChangedEvent
import com.piashcse.service.AppMetrics
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.common.emptyPage
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.retryQuery
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedList
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ConflictException
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
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

class PayoutRepositoryImpl : PayoutRepository {
    private fun SellerPayoutDAO.toResponse(
        orderNumbers: Map<String, String> = emptyMap(),
        sellerEmails: Map<String, String> = emptyMap(),
    ) = PayoutResponse(
        id = id.value,
        sellerId = sellerId.value,
        orderId = orderId.value,
        subTotal = subTotal.toPlainString(),
        commissionAmount = commissionAmount.toPlainString(),
        payoutAmount = payoutAmount.toPlainString(),
        currency = currency,
        status = status,
        payoutRequestId = payoutRequestId?.value,
        rejectionReason = rejectionReason,
        orderNumber = orderNumbers[orderId.value],
        sellerEmail = sellerEmails[sellerId.value],
        allowedActions =
            when (status) {
                PayoutStatus.REQUESTED.name -> listOf("APPROVE", "REJECT")
                PayoutStatus.APPROVED.name -> listOf("PAY", "REJECT")
                else -> emptyList()
            },
        requestedAt = requestedAt?.toUtcString(),
        approvedAt = approvedAt?.toUtcString(),
        rejectedAt = rejectedAt?.toUtcString(),
        paidAt = paidAt?.toUtcString(),
        createdAt = createdAt.toUtcString(),
    )

    private fun enrichPayouts(
        page: PaginatedResponse<PayoutResponse>,
        includeSellerEmail: Boolean,
    ): PaginatedResponse<PayoutResponse> {
        if (page.data.isEmpty()) return page
        val orderNumbers =
            OrderDAO.find { OrderTable.id inList page.data.map { it.orderId }.distinct().map { it.entityID(OrderTable) } }
                .associate { it.id.value to it.orderNumber }
        val sellerEmails =
            if (includeSellerEmail) {
                val sellerIds = page.data.map { it.sellerId }.distinct().map { it.entityID(SellerTable) }
                val sellerUsers =
                    SellerDAO.find { SellerTable.id inList sellerIds }
                        .associate { it.id.value to it.userId.value }
                UserDAO.find { UserTable.id inList sellerUsers.values.distinct().map { it.entityID(UserTable) } }
                    .associate { it.id.value to it.email }
                    .let { emails -> sellerUsers.mapValues { (_, userId) -> emails[userId].orEmpty() } }
            } else {
                emptyMap()
            }
        return page.copy(data = page.data.map { it.copy(orderNumber = orderNumbers[it.orderId], sellerEmail = sellerEmails[it.sellerId]) })
    }

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
            val pendingByCurrency =
                pendingRows
                    .groupBy { it.currency }
                    .mapValues { (_, rows) -> rows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) } }
            val primaryCurrency =
                byCurrency.keys.firstOrNull()
                    ?: pendingByCurrency.keys.firstOrNull()
                    ?: "USD"
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
    ): SellerStatementResponse =
        query {
            val seller = requireActiveSeller(sellerUserId)
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
                    ?: "USD"
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

    override suspend fun sellerPayouts(
        sellerUserId: String,
        limit: Int,
        offset: Int,
        status: PayoutStatus?,
        from: java.time.LocalDate?,
        to: java.time.LocalDate?,
    ) = query {
        val sellerIds = sellerIdsFor(sellerUserId)
        if (sellerIds.isEmpty()) {
            return@query emptyPage(limit, offset)
        }
        val page =
            SellerPayoutTable.selectAll().andWhere { SellerPayoutTable.sellerId inList sellerIds }
                .also { q ->
                    status?.let { q.andWhere { SellerPayoutTable.status eq it.name } }
                    from?.let { date -> q.andWhere { SellerPayoutTable.createdAt greaterEq date.atStartOfDay() } }
                    to?.let { date -> q.andWhere { SellerPayoutTable.createdAt lessEq date.atTime(java.time.LocalTime.MAX) } }
                    q.orderBy(SellerPayoutTable.createdAt to SortOrder.DESC)
                }.toPaginatedResponse(limit, offset) { SellerPayoutDAO.wrapRow(it).toResponse() }
        enrichPayouts(page, includeSellerEmail = false)
    }

    override suspend fun allPayouts(
        limit: Int,
        offset: Int,
        status: PayoutStatus?,
        sellerQuery: String?,
        from: java.time.LocalDate?,
        to: java.time.LocalDate?,
    ) = query {
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
            return@query emptyPage(limit, offset)
        }
        val page =
            SellerPayoutTable.selectAll()
                .also { q ->
                    status?.let { q.andWhere { SellerPayoutTable.status eq it.name } }
                    sellerIds?.let { q.andWhere { SellerPayoutTable.sellerId inList it } }
                    from?.let { date -> q.andWhere { SellerPayoutTable.createdAt greaterEq date.atStartOfDay() } }
                    to?.let { date -> q.andWhere { SellerPayoutTable.createdAt lessEq date.atTime(java.time.LocalTime.MAX) } }
                    q.orderBy(SellerPayoutTable.createdAt to SortOrder.DESC)
                }.toPaginatedResponse(limit, offset) { SellerPayoutDAO.wrapRow(it).toResponse() }
        enrichPayouts(page, includeSellerEmail = true)
    }

    private fun PayoutRequestDAO.toResponse(
        sellerEmails: Map<String, String> = emptyMap(),
        rowCount: Int = 0,
    ) = PayoutRequestResponse(
        id = id.value,
        sellerId = sellerId.value,
        sellerEmail = sellerEmails[sellerId.value],
        amount = amount.toPlainString(),
        currency = currency,
        status = status,
        rejectionReason = rejectionReason,
        rowCount = rowCount,
        allowedActions =
            when (status) {
                PayoutStatus.REQUESTED.name -> listOf("APPROVE", "REJECT")
                PayoutStatus.APPROVED.name -> listOf("PAY", "REJECT")
                else -> emptyList()
            },
        createdAt = createdAt.toUtcString(),
    )

    private fun payoutRequestResponses(
        requests: List<PayoutRequestDAO>,
        includeSellerEmail: Boolean,
    ): List<PayoutRequestResponse> {
        if (requests.isEmpty()) return emptyList()
        val counts =
            SellerPayoutDAO.find {
                SellerPayoutTable.payoutRequestId inList requests.map { it.id }
            }.groupingBy { it.payoutRequestId?.value }.eachCount()
        val emails =
            if (includeSellerEmail) {
                val sellerUsers =
                    SellerDAO.find { SellerTable.id inList requests.map { it.sellerId }.distinct() }
                        .associate { it.id.value to it.userId.value }
                UserDAO.find { UserTable.id inList sellerUsers.values.distinct().map { it.entityID(UserTable) } }
                    .associate { it.id.value to it.email }
                    .let { byUser -> sellerUsers.mapValues { (_, userId) -> byUser[userId].orEmpty() } }
            } else {
                emptyMap()
            }
        return requests.map { it.toResponse(emails, counts[it.id.value] ?: 0) }
    }

    override suspend fun sellerPayoutRequests(
        sellerUserId: String,
        status: PayoutStatus?,
        limit: Int,
        offset: Int,
    ) = query {
        val sellerIds = sellerIdsFor(sellerUserId)
        if (sellerIds.isEmpty()) {
            return@query emptyPage(limit, offset)
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

    override suspend fun allPayoutRequests(
        status: PayoutStatus?,
        sellerQuery: String?,
        limit: Int,
        offset: Int,
    ) = query {
        val sellerIds =
            sellerQuery?.takeIf { it.isNotBlank() }?.let { query ->
                val userIds =
                    UserDAO.find { UserTable.email like "%${query.trim().lowercase()}%" }
                        .map { it.id }
                if (userIds.isEmpty()) emptyList() else SellerDAO.find { SellerTable.userId inList userIds }.map { it.id }
            }
        if (sellerIds != null && sellerIds.isEmpty()) {
            return@query emptyPage(limit, offset)
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

    private suspend fun transitionRequest(
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

    override suspend fun approvePayoutRequest(requestId: String): PayoutRequestResponse =
        transitionRequest(requestId, PayoutStatus.APPROVED)

    override suspend fun rejectPayoutRequest(
        requestId: String,
        reason: String?,
    ): PayoutRequestResponse = transitionRequest(requestId, PayoutStatus.REJECTED, reason)

    override suspend fun payPayoutRequest(requestId: String): PayoutRequestResponse = transitionRequest(requestId, PayoutStatus.PAID)

    override suspend fun sellerPayoutRequestDetail(
        sellerUserId: String,
        requestId: String,
    ): PayoutRequestDetailResponse =
        query {
            val request = PayoutRequestDAO.findById(requestId) ?: requestId.throwNotFound("Payout request")
            if (request.sellerId !in sellerIdsFor(sellerUserId)) {
                throw ForbiddenException("Payout request belongs to another seller")
            }
            buildRequestDetail(request)
        }

    override suspend fun adminPayoutRequestDetail(requestId: String): PayoutRequestDetailResponse =
        query {
            val request = PayoutRequestDAO.findById(requestId) ?: requestId.throwNotFound("Payout request")
            buildRequestDetail(request)
        }

    private fun buildRequestDetail(request: PayoutRequestDAO): PayoutRequestDetailResponse {
        val rows = SellerPayoutDAO.find { SellerPayoutTable.payoutRequestId eq request.id }.toList()
        val page =
            PaginatedResponse(
                rows.map { it.toResponse() },
                PaginationMetadata(rows.size.toLong(), rows.size.coerceAtLeast(1), 0),
            )
        val enriched = enrichPayouts(page, includeSellerEmail = true)
        return PayoutRequestDetailResponse(
            payoutRequestResponses(listOf(request), includeSellerEmail = true).single(),
            enriched.data,
        )
    }

    override suspend fun markPaid(
        payoutId: String,
        actorId: String?,
    ) = query {
        val payout =
            SellerPayoutDAO.find { SellerPayoutTable.id eq payoutId.entityID(SellerPayoutTable) }
                .forUpdate()
                .firstOrNull() ?: payoutId.throwNotFound("Payout")
        if (payout.status == PayoutStatus.PAID.name) return@query payout.toResponse()
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
        payout.toResponse()
    }

    override suspend fun requestPayout(
        sellerUserId: String,
        amount: BigDecimal,
        requestKey: String?,
        currency: String?,
    ): List<PayoutResponse> {
        val rows =
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

    override suspend fun approvePayout(payoutId: String) =
        query {
            val payout =
                SellerPayoutDAO.find { SellerPayoutTable.id eq payoutId.entityID(SellerPayoutTable) }
                    .forUpdate()
                    .firstOrNull() ?: payoutId.throwNotFound("Payout")
            if (payout.status == PayoutStatus.APPROVED.name) return@query payout.toResponse()
            if (payout.status != PayoutStatus.REQUESTED.name) {
                throw ConflictException("Payout $payoutId cannot be approved from status ${payout.status}")
            }
            payout.status = PayoutStatus.APPROVED.name
            payout.approvedAt = currentUtc()
            payout.payoutRequestId?.let { requestId ->
                PayoutRequestDAO.findById(requestId.value)?.status = PayoutStatus.APPROVED.name
            }
            payout.toResponse()
        }

    override suspend fun rejectPayout(
        payoutId: String,
        reason: String?,
    ) = query {
        val payout =
            SellerPayoutDAO.find { SellerPayoutTable.id eq payoutId.entityID(SellerPayoutTable) }
                .forUpdate()
                .firstOrNull() ?: payoutId.throwNotFound("Payout")
        if (payout.status == PayoutStatus.REJECTED.name) return@query payout.toResponse()
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
        payout.toResponse()
    }
}
