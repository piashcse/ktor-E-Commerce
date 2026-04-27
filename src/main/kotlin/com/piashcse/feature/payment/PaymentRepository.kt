package com.piashcse.feature.payment

import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse

interface PaymentRepository {
    /**
     * Processes a new payment.
     *
     * @param paymentRequest The payment details.
     * @return The created payment record.
     */
    suspend fun createPayment(paymentRequest: PaymentRequest): PaymentResponse

    /**
     * Retrieves payment details by payment ID.
     *
     * @param paymentId The unique identifier of the payment.
     * @return The payment details.
     */
    suspend fun getPaymentById(paymentId: String): PaymentResponse

    /**
     * Retrieves all payments for a specific order.
     *
     * @param orderId The unique identifier of the order.
     * @return A list of payments for the order.
     */
    suspend fun getPaymentsByOrderId(orderId: String, limit: Int = 20, offset: Int = 0): PaginatedResponse<PaymentResponse>
}