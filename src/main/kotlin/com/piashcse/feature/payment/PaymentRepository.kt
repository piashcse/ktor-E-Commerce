package com.piashcse.feature.payment

import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.Payment

interface PaymentRepository {
    /**
     * Processes a new payment.
     *
     * @param paymentRequest The payment details.
     * @return The created payment record.
     */
    suspend fun createPayment(paymentRequest: PaymentRequest): Payment

    /**
     * Retrieves payment details by payment ID.
     *
     * @param paymentId The unique identifier of the payment.
     * @return The payment details.
     */
    suspend fun getPaymentById(paymentId: String): Payment
}