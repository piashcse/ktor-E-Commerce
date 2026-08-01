package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.UserType
import com.piashcse.event.EventBus
import com.piashcse.event.PaymentCompletedEvent
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.money.Money
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import java.math.BigDecimal

class PaymentService(private val paymentRepo: PaymentRepository) {

    /**
     * Processes a payment for the caller's own order. The order ownership is
     * enforced and the payment status is derived server-side (a real gateway
     * would confirm the charge); client-supplied status is never trusted.
     * Runs in a retryable transaction; publishes an after-commit event when the
     * order becomes fully paid.
     */
    suspend fun createPayment(
        callerUserId: String,
        paymentRequest: PaymentRequest,
    ): PaymentResponse = suspendRetryQuery {
        paymentRequest.transactionId?.let { txId ->
            paymentRepo.getPaymentByTransactionId(txId)?.let { existing ->
                if (existing.orderId == paymentRequest.orderId) return@suspendRetryQuery existing
            }
        }

        val order = paymentRepo.getOrderForPayment(paymentRequest.orderId)
        if (order.userId != callerUserId) throw ForbiddenException(Message.Payments.NOT_ORDER_OWNER)

        val paymentAmount = paymentRequest.amount
        if (paymentAmount.compareTo(order.orderTotal) != 0) {
            throw ValidationException(
                Message.Payments.amountMismatch(paymentAmount.toPlainString(), order.orderTotal.toPlainString()),
            )
        }

        val paidAmount = paymentRepo.getCompletedPaymentsSum(paymentRequest.orderId)
        if (paidAmount.compareTo(order.orderTotal) >= 0) {
            throw ValidationException(Message.Payments.ALREADY_PAID)
        }

        val payment = paymentRepo.createPayment(
            orderId = paymentRequest.orderId,
            userId = order.userId,
            amount = paymentAmount,
            paymentMethod = paymentRequest.paymentMethod,
            transactionId = paymentRequest.transactionId,
            status = PaymentStatus.COMPLETED,
        )

        if (Money.round(paidAmount.add(paymentAmount)).compareTo(order.orderTotal) >= 0) {
            paymentRepo.finalizeOrderPayment(paymentRequest.orderId)
            EventBus.publish(
                PaymentCompletedEvent(
                    paymentId = payment.id,
                    orderId = paymentRequest.orderId,
                    userId = order.userId,
                    email = paymentRepo.getUserEmail(order.userId).orEmpty(),
                    amount = BigDecimal(payment.amount),
                ),
            )
        }

        payment
    }

    /**
     * Returns a single payment if the caller owns the order or is an admin.
     */
    suspend fun getPaymentById(
        callerUserId: String,
        userType: UserType,
        paymentId: String,
    ): PaymentResponse {
        val payment = paymentRepo.getPaymentById(paymentId)
        ensurePaymentViewAccess(callerUserId, userType, payment.orderId)
        return payment
    }

    /**
     * Returns the payments of an order if the caller owns the order or is an
     * admin.
     */
    suspend fun getPaymentsByOrderId(
        callerUserId: String,
        userType: UserType,
        orderId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<PaymentResponse> {
        ensurePaymentViewAccess(callerUserId, userType, orderId)
        return paymentRepo.getPaymentsByOrderId(orderId, limit, offset)
    }

    private suspend fun ensurePaymentViewAccess(callerUserId: String, userType: UserType, orderId: String) {
        val ownerId = paymentRepo.getOrderOwnerId(orderId) ?: orderId.throwNotFound("Order")
        if (ownerId != callerUserId && !userType.isAdminOrHigher) {
            throw ForbiddenException(Message.Payments.NOT_PAYMENT_VIEWER)
        }
    }
}
