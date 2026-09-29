package com.piashcse.feature.payment

import java.math.BigDecimal

/**
 * Payment gateway abstraction — current manual ledger is CASH_ON_DELIVERY.
 * Real providers (Stripe/SSLCommerz/bKash) implement this; routes stay unchanged.
 */
interface PaymentGateway {
    val name: String
    suspend fun charge(orderId: String, amount: BigDecimal, transactionId: String?): GatewayChargeResult
    suspend fun verifyWebhook(payload: String, signature: String?): Boolean
}

data class GatewayChargeResult(
    val success: Boolean,
    val gatewayTransactionId: String?,
    val message: String? = null,
)

class ManualPaymentGateway : PaymentGateway {
    override val name: String = "CASH_ON_DELIVERY"
    override suspend fun charge(orderId: String, amount: BigDecimal, transactionId: String?) =
        GatewayChargeResult(success = true, gatewayTransactionId = transactionId, message = "Manual payment recorded")
    override suspend fun verifyWebhook(payload: String, signature: String?) = true
}
