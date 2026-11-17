package com.piashcse.feature.payout

import com.piashcse.constants.PayoutStatus
import com.piashcse.utils.common.PaginatedResponse
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotNull
import org.valiktor.validate
import java.math.BigDecimal

@Serializable
data class PayoutResponse(
    val id: String,
    val sellerId: String,
    val orderId: String,
    val subTotal: String,
    val commissionAmount: String,
    val payoutAmount: String,
    val currency: String = "USD",
    val status: String,
    val payoutRequestId: String? = null,
    val rejectionReason: String? = null,
    val orderNumber: String? = null,
    val sellerEmail: String? = null,
    val allowedActions: List<String> = emptyList(),
    val requestedAt: String? = null,
    val approvedAt: String? = null,
    val rejectedAt: String? = null,
    val paidAt: String? = null,
    val createdAt: String,
)

@Serializable
data class PayoutRequestResponse(
    val id: String,
    val sellerId: String,
    val sellerEmail: String? = null,
    val amount: String,
    val currency: String,
    val status: String,
    val rejectionReason: String? = null,
    val rowCount: Int = 0,
    val allowedActions: List<String> = emptyList(),
    val createdAt: String?,
)

@Serializable
data class PayoutRequestDetailResponse(
    val request: PayoutRequestResponse,
    val rows: List<PayoutResponse> = emptyList(),
)

@Serializable
data class PayoutDecisionRequest(val reason: String? = null)

@Serializable
data class PayoutBalanceResponse(
    val available: String,
    val currency: String = "USD",
    val pendingRequestAmount: String = "0.00",
    /** Available balance per ISO currency (multi-currency sellers). */
    val balances: Map<String, String> = emptyMap(),
)

@Serializable
data class SellerStatementRow(
    val payoutId: String,
    val orderId: String,
    val subTotal: String,
    val commissionAmount: String,
    val payoutAmount: String,
    val currency: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class SellerStatementResponse(
    val from: String?,
    val to: String?,
    val currency: String,
    val grossSales: String,
    val commission: String,
    val netPayout: String,
    val paid: String,
    val pending: String,
    val available: String,
    val taxCollected: String,
    val rowCount: Int,
    val rows: List<SellerStatementRow>,
)

interface PayoutRepository {
    suspend fun availableBalance(sellerUserId: String): PayoutBalanceResponse

    suspend fun statement(
        sellerUserId: String,
        from: String?,
        to: String?,
    ): SellerStatementResponse

    suspend fun sellerPayouts(
        sellerUserId: String,
        limit: Int,
        offset: Int,
        status: PayoutStatus? = null,
        from: java.time.LocalDate? = null,
        to: java.time.LocalDate? = null,
    ): PaginatedResponse<PayoutResponse>

    suspend fun allPayouts(
        limit: Int,
        offset: Int,
        status: PayoutStatus? = null,
        sellerQuery: String? = null,
        from: java.time.LocalDate? = null,
        to: java.time.LocalDate? = null,
    ): PaginatedResponse<PayoutResponse>

    suspend fun sellerPayoutRequests(
        sellerUserId: String,
        status: PayoutStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PayoutRequestResponse>

    suspend fun allPayoutRequests(
        status: PayoutStatus?,
        sellerQuery: String?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PayoutRequestResponse>

    suspend fun approvePayoutRequest(requestId: String): PayoutRequestResponse

    suspend fun rejectPayoutRequest(
        requestId: String,
        reason: String?,
    ): PayoutRequestResponse

    suspend fun payPayoutRequest(requestId: String): PayoutRequestResponse

    suspend fun sellerPayoutRequestDetail(
        sellerUserId: String,
        requestId: String,
    ): PayoutRequestDetailResponse

    suspend fun adminPayoutRequestDetail(requestId: String): PayoutRequestDetailResponse

    suspend fun markPaid(
        payoutId: String,
        actorId: String? = null,
    ): PayoutResponse

    suspend fun requestPayout(
        sellerUserId: String,
        amount: BigDecimal,
        requestKey: String?,
        currency: String? = null,
    ): List<PayoutResponse>

    suspend fun approvePayout(payoutId: String): PayoutResponse

    suspend fun rejectPayout(
        payoutId: String,
        reason: String? = null,
    ): PayoutResponse
}

@Serializable
data class PayoutRequest(
    @Contextual val amount: BigDecimal,
    /** ISO currency; required when the seller has balances in multiple currencies. */
    val currency: String? = null,
) {
    init {
        validate(this) {
            validate(PayoutRequest::amount).isNotNull().isGreaterThan(BigDecimal.ZERO)
        }
    }
}
