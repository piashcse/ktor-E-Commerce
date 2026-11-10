package com.piashcse.feature.payment

import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse

interface PaymentRepository {
    /**
     * Creates a payment intent. The server owns the resulting status: COD stays
     * PENDING until an admin confirms collection; a configured gateway may capture
     * immediately. The client can never submit a status.
     */
    suspend fun createPayment(
        paymentRequest: PaymentRequest,
        callerUserId: String,
        ipAddress: String? = null,
    ): PaymentResponse

    /**
     * Explicitly confirms a pending payment (admin / provider webhook). Completes
     * the order and finalizes stock reservations when the order is fully paid.
     */
    suspend fun confirmPayment(
        paymentId: String,
        actorUserId: String?,
    ): PaymentResponse

    /**
     * Retrieves payment details by payment ID.
     */
    suspend fun getPaymentById(
        paymentId: String,
        callerUserId: String,
    ): PaymentResponse

    /**
     * Retrieves all payments for a specific order.
     */
    suspend fun getPaymentsByOrderId(
        orderId: String,
        callerUserId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<PaymentResponse>

    /**
     * Verifies and applies a provider webhook. Deliveries are deduplicated by
     * `(provider, event_id)` so at-least-once retries are safe.
     */
    suspend fun handleWebhook(
        provider: String,
        payload: String,
        signature: String?,
    ): WebhookResult
}

data class WebhookResult(
    val accepted: Boolean,
    val duplicate: Boolean = false,
    val message: String,
)
