package com.piashcse.feature.payout

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
    val requestedAt: String? = null,
    val approvedAt: String? = null,
    val rejectedAt: String? = null,
    val paidAt: String? = null,
    val createdAt: String,
)

@Serializable
data class PayoutBalanceResponse(
    val available: String,
    val currency: String = "USD",
    val pendingRequestAmount: String = "0.00",
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
    ): PaginatedResponse<PayoutResponse>

    suspend fun allPayouts(
        limit: Int,
        offset: Int,
        status: String?,
    ): PaginatedResponse<PayoutResponse>

    suspend fun markPaid(
        payoutId: String,
        actorId: String? = null,
    ): PayoutResponse

    suspend fun requestPayout(
        sellerUserId: String,
        amount: BigDecimal,
        requestKey: String?,
    ): List<PayoutResponse>

    suspend fun approvePayout(payoutId: String): PayoutResponse

    suspend fun rejectPayout(payoutId: String): PayoutResponse
}

@Serializable
data class PayoutRequest(
    @Contextual val amount: BigDecimal,
) {
    init {
        validate(this) {
            validate(PayoutRequest::amount).isNotNull().isGreaterThan(BigDecimal.ZERO)
        }
    }
}
