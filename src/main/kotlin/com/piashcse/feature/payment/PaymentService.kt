package com.piashcse.feature.payment

import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.PaymentTable
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.Payment
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Controller for managing payment-related operations.
 */
class PaymentService : PaymentRepository {

    /**
     * Creates a new payment for an order based on the provided payment details.
     *
     * @param paymentRequest The details of the payment including order ID, amount, status, and payment method.
     * @return The created payment entity.
     * @throws Exception if the order with the provided order ID is not found.
     */
    override suspend fun createPayment(paymentRequest: PaymentRequest): Payment = query {
        val isOrderExist = OrderDAO.find { OrderTable.id eq paymentRequest.orderId }.toList().singleOrNull()
        isOrderExist?.let {
            PaymentDAO.new {
                orderId = EntityID(paymentRequest.orderId, PaymentTable)
                userId = it.userId
                amount = BigDecimal.valueOf(paymentRequest.amount)
                status = paymentRequest.status
                paymentMethod = paymentRequest.paymentMethod
                transactionId = paymentRequest.transactionId
                currency = paymentRequest.currency
            }.response()
        } ?: throw paymentRequest.orderId.notFoundException()
    }

    /**
     * Retrieves a payment by its ID.
     *
     * @param paymentId The ID of the payment to retrieve.
     * @return The payment entity associated with the provided payment ID.
     * @throws Exception if no payment is found for the given payment ID.
     */
    override suspend fun getPaymentById(paymentId: String): Payment = query {
        val payment = PaymentDAO.find { PaymentTable.id eq paymentId }.toList().firstOrNull()
        payment?.response() ?: throw paymentId.notFoundException()
    }

    /**
     * Retrieves a payment by order ID.
     *
     * @param orderId The ID of the order.
     * @return The payment entity associated with the order, or null if not found.
     */
    suspend fun getPaymentByOrderId(orderId: String): Payment? = query {
        val payment = PaymentDAO.find { PaymentTable.orderId eq orderId }.toList().firstOrNull()
        payment?.response()
    }

    /**
     * Marks a payment as paid.
     *
     * @param paymentId The ID of the payment.
     * @param transactionId The transaction ID from the payment gateway.
     * @return The updated payment.
     */
    suspend fun markAsPaid(paymentId: String, transactionId: String?): Payment = query {
        val payment = PaymentDAO.find { PaymentTable.id eq paymentId }.singleOrNull()
            ?: throw paymentId.notFoundException()

        payment.status = com.piashcse.constants.PaymentStatus.COMPLETED
        payment.transactionId = transactionId ?: payment.transactionId
        payment.paidAt = LocalDateTime.now()
        payment.response()
    }

    /**
     * Processes a refund for a payment.
     *
     * @param paymentId The ID of the payment.
     * @param refundAmount The amount to refund (null for full refund).
     * @param reason The reason for the refund.
     * @return The updated payment.
     */
    suspend fun processRefund(paymentId: String, refundAmount: Double?, reason: String?): Payment = query {
        val payment = PaymentDAO.find { PaymentTable.id eq paymentId }.singleOrNull()
            ?: throw paymentId.notFoundException()

        val refundAmt = refundAmount ?: payment.amount.toDouble()
        payment.refundAmount = BigDecimal.valueOf(refundAmt)
        payment.refundReason = reason
        payment.status = com.piashcse.constants.PaymentStatus.REFUNDED
        payment.response()
    }
}