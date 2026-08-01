package com.piashcse.feature.refund_request

import com.piashcse.constants.UserType
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.utils.common.PaginatedResponse

class RefundRequestService(private val refundRequestRepo: RefundRequestRepository) {
    suspend fun createRefundRequest(
        userId: String,
        orderId: String,
        request: RefundRequestRequest,
    ): RefundRequestResponse = refundRequestRepo.createRefundRequest(userId, orderId, request)

    suspend fun getRefundsByOrderId(
        orderId: String,
        userId: String,
        userType: UserType,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<RefundRequestResponse> = refundRequestRepo.getRefundsByOrderId(orderId, userId, userType, limit, offset)

    suspend fun getRefundById(
        refundId: String,
        userId: String,
        userType: UserType,
    ): RefundRequestResponse? = refundRequestRepo.getRefundById(refundId, userId, userType)

    suspend fun updateRefundStatus(
        refundId: String,
        request: UpdateRefundStatusRequest,
        userId: String,
    ): RefundRequestResponse = refundRequestRepo.updateRefundStatus(refundId, request, userId)

    suspend fun shipRefund(
        refundId: String,
        request: ShipRefundRequest,
        userId: String,
    ): RefundRequestResponse = refundRequestRepo.shipRefund(refundId, request, userId)
}
