package com.piashcse.feature.refund_request

import com.piashcse.constants.RefundStatus
import com.piashcse.constants.UserType
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.utils.common.PaginatedResponse
import java.time.Instant

interface RefundRequestRepository {
    suspend fun createRefundRequest(
        userId: String,
        orderId: String,
        request: RefundRequestRequest,
    ): RefundRequestResponse

    suspend fun getRefundsByOrderId(
        orderId: String,
        userId: String,
        userType: UserType,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<RefundRequestResponse>

    /** Seller queue for refunds belonging to their shop. */
    suspend fun listForSeller(
        sellerUserId: String,
        status: RefundStatus?,
        search: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<RefundRequestResponse>

    /** Admin queue across all shops. */
    suspend fun listAll(
        status: RefundStatus?,
        search: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<RefundRequestResponse>

    suspend fun getRefundById(
        refundId: String,
        userId: String,
        userType: UserType,
    ): RefundRequestResponse?

    suspend fun updateRefundStatus(
        refundId: String,
        request: UpdateRefundStatusRequest,
        userId: String,
    ): RefundRequestResponse

    suspend fun shipRefund(
        refundId: String,
        request: ShipRefundRequest,
        userId: String,
    ): RefundRequestResponse

    /** Appends a private evidence image URL to an open refund request. */
    suspend fun addRefundEvidence(
        refundId: String,
        userId: String,
        imageUrl: String,
    ): RefundRequestResponse
}
