package com.piashcse.feature.payout

import com.piashcse.utils.common.PaginatedResponse
import kotlinx.serialization.Serializable

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

    suspend fun markPaid(payoutId: String): PayoutResponse
}
