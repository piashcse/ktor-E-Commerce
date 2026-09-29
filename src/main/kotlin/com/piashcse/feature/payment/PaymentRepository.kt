package com.piashcse.feature.payment

import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse

interface PaymentRepository {
    /**
     * Processes a new payment.
     *
     * @param paymentRequest The payment details.
     * @param callerUserId Authenticated caller — must own the order.
     * @return The created payment record.
     */
    suspend fun createPayment(
        paymentRequest: PaymentRequest,
        callerUserId: String,
    ): PaymentResponse

    /**
     * Retrieves payment details by payment ID.
     *
     * @param paymentId The unique identifier of the payment.
     * @param callerUserId Authenticated caller — must own the payment's order.
     * @return The payment details.
     */
    suspend fun getPaymentById(
        paymentId: String,
        callerUserId: String,
    ): PaymentResponse

    /**
     * Retrieves all payments for a specific order.
     *
     * @param orderId The unique identifier of the order.
     * @param callerUserId Authenticated caller — must own the order.
     * @return A list of payments for the order.
     */
    suspend fun getPaymentsByOrderId(
        orderId: String,
        callerUserId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<PaymentResponse>
}
