package com.piashcse.feature.refund_request

import com.piashcse.constants.RefundStatus
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.utils.common.PaginatedResponse
import java.math.BigDecimal

/**
 * Facts about a refund request used by the service layer to enforce authorization.
 */
data class RefundAccess(
    val refundId: String,
    val orderId: String,
    val orderItemId: String,
    val currentStatus: RefundStatus,
    val currentRefundAmount: BigDecimal?,
    val maxRefundAmount: BigDecimal,
    val isCustomer: Boolean,
    val isSeller: Boolean,
    val isAdmin: Boolean,
)

/**
 * Facts about an order used by the service layer to authorize refund operations.
 */
data class RefundOrderAccess(
    val orderId: String,
    val isCustomer: Boolean,
    val isSeller: Boolean,
    val isAdmin: Boolean,
)

/**
 * Persistence boundary for the Refund Request aggregate.
 *
 * The repository is limited to data access (reads/writes + projection to DTOs).
 * All authorization, domain rules, validation and transaction orchestration live
 * in [RefundRequestService].
 */
interface RefundRequestRepository {
    /**
     * Resolves the authorization facts for a refund request relative to [userId].
     * Throws if the refund request does not exist.
     */
    suspend fun getRefundAccess(
        userId: String,
        refundId: String,
    ): RefundAccess

    /**
     * Resolves the authorization facts for an order relative to [userId].
     * Throws if the order does not exist.
     */
    suspend fun getRefundOrderAccess(
        userId: String,
        orderId: String,
    ): RefundOrderAccess

    /**
     * Persists a new PENDING refund request. Assumes the caller has already
     * authorized the customer and validated the order item.
     */
    suspend fun createRefundRequest(
        userId: String,
        orderId: String,
        request: RefundRequestRequest,
    ): RefundRequestResponse

    /**
     * Lists refund requests for an order. Assumes the caller has authorized the order.
     */
    suspend fun getRefundsByOrderId(
        orderId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RefundRequestResponse>

    /**
     * Loads a single refund request, or null when it does not exist.
     */
    suspend fun getRefundById(refundId: String): RefundRequestResponse?

    /**
     * Applies a status transition, refund amount/method and order payment-status
     * side effects. The caller is responsible for authorization and transition rules.
     */
    suspend fun updateRefundStatus(
        refundId: String,
        request: UpdateRefundStatusRequest,
    ): RefundRequestResponse

    /**
     * Marks an approved refund as shipped. The caller is responsible for
     * authorization and the approved-status rule.
     */
    suspend fun shipRefund(
        refundId: String,
        request: ShipRefundRequest,
    ): RefundRequestResponse
}
