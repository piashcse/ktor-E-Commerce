package com.piashcse.feature.payout

import com.piashcse.constants.PayoutStatus
import java.math.BigDecimal

class PayoutRepositoryImpl : PayoutRepository {
    override suspend fun availableBalance(sellerUserId: String): PayoutBalanceResponse = PayoutBalanceService.availableBalance(sellerUserId)

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
    ): List<PayoutResponse> = PayoutBalanceService.requestPayout(sellerUserId, amount, requestKey, currency)

    override suspend fun approvePayout(payoutId: String) = PayoutSettlementService.approve(payoutId)

    override suspend fun rejectPayout(
        payoutId: String,
        reason: String?,
    ) = PayoutSettlementService.reject(payoutId, reason)
}
