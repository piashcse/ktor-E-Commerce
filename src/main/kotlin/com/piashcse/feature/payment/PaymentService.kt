package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.event.EventBus
import com.piashcse.event.PaymentCompletedEvent
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.money.Money
import com.piashcse.utils.validator.ValidationException
import java.math.BigDecimal

class PaymentService(private val paymentRepo: PaymentRepository) {

    /**
     * Processes a payment, enforcing idempotency, amount matching and
     * overpayment rules. Runs in a retryable transaction; publishes an
     * after-commit event when the order becomes fully paid.
     */
    suspend fun createPayment(paymentRequest: PaymentRequest): PaymentResponse = suspendRetryQuery {
        paymentRequest.transactionId?.let { txId ->
            paymentRepo.getPaymentByTransactionId(txId)?.let { return@suspendRetryQuery it }
        }

        val order = paymentRepo.getOrderForPayment(paymentRequest.orderId)
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

        val payment = paymentRepo.createPayment(paymentRequest.orderId, order.userId, paymentRequest)

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

    suspend fun getPaymentById(paymentId: String): PaymentResponse = paymentRepo.getPaymentById(paymentId)

    suspend fun getPaymentsByOrderId(
        orderId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<PaymentResponse> = paymentRepo.getPaymentsByOrderId(orderId, limit, offset)
}
