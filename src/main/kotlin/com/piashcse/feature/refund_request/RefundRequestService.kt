package com.piashcse.feature.refund_request

import com.piashcse.constants.Message
import com.piashcse.constants.RefundStatus
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import java.math.BigDecimal

class RefundRequestService(private val refundRequestRepo: RefundRequestRepository) {

    /**
     * Creates a refund request for an order item. Only the customer who owns the
     * order may request a refund. Runs in a retryable transaction.
     */
    suspend fun createRefundRequest(
        userId: String,
        orderId: String,
        request: RefundRequestRequest,
    ): RefundRequestResponse = suspendRetryQuery {
        val access = refundRequestRepo.getRefundOrderAccess(userId, orderId)
        if (!access.isCustomer) throw ForbiddenException(Message.Orders.UNAUTHORIZED)
        if (!access.isOrderPaid) throw ValidationException(Message.Refunds.ORDER_NOT_PAID)

        refundRequestRepo.createRefundRequest(userId, orderId, request)
    }

    /**
     * Lists refund requests for an order. Customers, the order's seller and
     * admins may view them.
     */
    suspend fun getRefundsByOrderId(
        orderId: String,
        userId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<RefundRequestResponse> = suspendRetryQuery {
        val access = refundRequestRepo.getRefundOrderAccess(userId, orderId)
        if (!access.isCustomer && !access.isSeller && !access.isAdmin)
            throw ForbiddenException(Message.Orders.UNAUTHORIZED)

        refundRequestRepo.getRefundsByOrderId(orderId, limit, offset)
    }

    /**
     * Loads a single refund request after authorizing the caller. Returns null
     * when the refund request does not exist.
     */
    suspend fun getRefundById(
        refundId: String,
        userId: String,
    ): RefundRequestResponse? = suspendRetryQuery {
        val response = refundRequestRepo.getRefundById(refundId) ?: return@suspendRetryQuery null

        val access = refundRequestRepo.getRefundAccess(userId, refundId)
        if (!access.isCustomer && !access.isSeller && !access.isAdmin)
            throw ForbiddenException(Message.Orders.UNAUTHORIZED)

        response
    }

    /**
     * Updates a refund status. Only the order's seller and admins may update the
     * status; the refund amount may not exceed the order item total and must be
     * present when marking a refund as REFUNDED. Runs in a retryable transaction.
     */
    suspend fun updateRefundStatus(
        refundId: String,
        request: UpdateRefundStatusRequest,
        userId: String,
    ): RefundRequestResponse = suspendRetryQuery {
        val access = refundRequestRepo.getRefundAccess(userId, refundId)
        if (!access.isSeller && !access.isAdmin) throw ForbiddenException(Message.Errors.FORBIDDEN)

        if (!RefundStatus.canTransitionTo(access.currentStatus, request.status)) {
            throw ValidationException(Message.Refunds.invalidTransition(access.currentStatus.name, request.status.name))
        }

        request.refundAmount?.let { amount ->
            if (amount <= BigDecimal.ZERO) {
                throw ValidationException(Message.Refunds.AMOUNT_NOT_POSITIVE)
            }
            val otherRefunds = access.alreadyRefundedAmount - (access.currentRefundAmount ?: BigDecimal.ZERO)
            val cap = access.maxRefundAmount - otherRefunds
            if (amount > cap) {
                throw ValidationException(Message.Refunds.AMOUNT_EXCEEDS_ITEM_TOTAL)
            }
        }

        if (request.status == RefundStatus.REFUNDED && request.refundAmount == null && access.currentRefundAmount == null) {
            throw ValidationException(Message.Refunds.REFUND_AMOUNT_REQUIRED)
        }

        refundRequestRepo.updateRefundStatus(refundId, request)
    }

    /**
     * Marks an approved refund as shipped. Only the customer who owns the refund
     * may ship it, and only after the seller has approved it.
     */
    suspend fun shipRefund(
        refundId: String,
        request: ShipRefundRequest,
        userId: String,
    ): RefundRequestResponse = suspendRetryQuery {
        val access = refundRequestRepo.getRefundAccess(userId, refundId)
        if (!access.isCustomer) throw ForbiddenException(Message.Orders.UNAUTHORIZED)
        if (access.currentStatus != RefundStatus.APPROVED) {
            throw ValidationException(Message.Refunds.MUST_BE_APPROVED)
        }

        refundRequestRepo.shipRefund(refundId, request)
    }
}
