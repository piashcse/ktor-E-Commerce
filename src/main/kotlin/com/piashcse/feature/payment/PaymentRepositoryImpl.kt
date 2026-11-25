package com.piashcse.feature.payment

import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.*
import com.piashcse.feature.fraud.FraudService
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.refund_request.RefundRequestRepository
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.feature.subscription.SubscriptionBillingService
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.resilience.CircuitBreaker

/**
 * Payment ledger.
 *
 * Security invariants:
 * - The caller can never submit a payment status; the server decides.
 * - A payment can only be created against a payable order owned by the caller.
 * - An order is only marked PAID after the completed payments cover the order total.
 * - Payment confirmation is an explicit operation (admin or provider webhook).
 */
class PaymentRepositoryImpl(
    private val gateway: PaymentGateway,
    private val fraudService: FraudService,
    private val loyaltyService: LoyaltyService,
    private val digitalDelivery: DigitalDeliveryService,
    private val subscriptionBilling: SubscriptionBillingService,
    private val refunds: RefundRequestRepository,
) : PaymentRepository {
    private val gatewayBreaker = CircuitBreaker("payment-gateway", failureThreshold = 5, cooldownMs = 30_000)

    private val intents =
        PaymentIntentService(
            gateway,
            fraudService,
            loyaltyService,
            digitalDelivery,
            gatewayBreaker,
        )

    private val webhooks =
        PaymentWebhookService(
            gateway,
            refunds,
            subscriptionBilling,
            loyaltyService,
            digitalDelivery,
            gatewayBreaker,
        )

    override suspend fun createPayment(
        paymentRequest: PaymentRequest,
        callerUserId: String,
        ipAddress: String?,
    ): PaymentResponse = intents.createPayment(paymentRequest, callerUserId, ipAddress)

    override suspend fun releaseReview(
        paymentId: String,
        actorUserId: String,
    ): PaymentResponse = intents.releaseReview(paymentId, actorUserId)

    override suspend fun confirmPayment(
        paymentId: String,
        actorUserId: String?,
    ): PaymentResponse = intents.confirmPayment(paymentId, actorUserId)

    override suspend fun handleWebhook(
        provider: String,
        payload: String,
        signature: String?,
    ): WebhookResult = webhooks.handleWebhook(provider, payload, signature)

    override suspend fun replayWebhook(
        eventId: String,
        actorUserId: String,
    ): WebhookResult = webhooks.replayWebhook(eventId, actorUserId)

    override suspend fun getPaymentById(
        paymentId: String,
        callerUserId: String,
    ): PaymentResponse = PaymentQueries.getPaymentById(paymentId, callerUserId)

    override suspend fun getPaymentsByOrderId(
        orderId: String,
        callerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PaymentResponse> = PaymentQueries.getPaymentsByOrderId(orderId, callerUserId, limit, offset)

    override suspend fun listPayments(
        status: PaymentStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PaymentResponse> = PaymentQueries.listPayments(status, limit, offset)
}
