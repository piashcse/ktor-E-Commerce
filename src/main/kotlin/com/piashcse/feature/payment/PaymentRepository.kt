package com.piashcse.feature.payment

import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse
import java.math.BigDecimal

/**
 * Read facts needed by [PaymentService] to validate a payment while holding a
 * row lock on the order.
 */
data class OrderPaymentInfo(
    val orderId: String,
    val userId: String,
    val orderTotal: BigDecimal,
)

/**
 * Persistence boundary for the Payment feature.
 *
 * The repository is limited to data access (reads/writes + projection).
 * Payment validation rules, overpayment checks and domain event publishing
 * live in [PaymentService].
 */
interface PaymentRepository {
    /**
     * Returns an existing payment for a transaction id (idempotency lookup).
     */
    suspend fun getPaymentByTransactionId(transactionId: String): PaymentResponse?

    /**
     * Locks the order row for payment processing and returns payment facts.
     */
    suspend fun getOrderForPayment(orderId: String): OrderPaymentInfo

    /**
     * Sum of completed payments for an order.
     */
    suspend fun getCompletedPaymentsSum(orderId: String): BigDecimal

    /**
     * Persists a new payment record.
     */
    suspend fun createPayment(
        orderId: String,
        userId: String,
        request: PaymentRequest,
    ): PaymentResponse

    /**
     * Marks an order as fully paid and finalizes its stock reservations.
     */
    suspend fun finalizeOrderPayment(orderId: String)

    /**
     * Resolves the email of the order's owner (for after-commit notifications).
     */
    suspend fun getUserEmail(userId: String): String?

    /**
     * Retrieves payment details by payment ID.
     */
    suspend fun getPaymentById(paymentId: String): PaymentResponse

    /**
     * Retrieves all payments for a specific order.
     */
    suspend fun getPaymentsByOrderId(
        orderId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<PaymentResponse>
}
