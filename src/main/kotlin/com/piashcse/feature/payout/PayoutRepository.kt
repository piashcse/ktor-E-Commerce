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
    val status: String,
    val createdAt: String,
)

interface PayoutRepository {
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
