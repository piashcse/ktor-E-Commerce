package com.piashcse.feature.payment

import com.piashcse.constants.PaymentMethod
import java.math.BigDecimal

/**
 * Payment gateway abstraction. Real providers (Stripe/SSLCommerz/bKash) implement
 * this interface. Until a provider is configured only CASH_ON_DELIVERY is allowed:
 * the charge is recorded as PENDING and must be confirmed by an admin when cash is
 * collected. This prevents a client from self-declaring a successful payment.
 */
interface PaymentGateway {
    val name: String

    suspend fun charge(
        orderId: String,
        amount: BigDecimal,
        paymentMethod: PaymentMethod,
        transactionId: String?,
    ): GatewayChargeResult

    suspend fun verifyWebhook(
        payload: String,
        signature: String?,
    ): Boolean
}

data class GatewayChargeResult(
    val success: Boolean,
    /** True when the provider captured money immediately (card/wallet), false for COD/manual. */
    val captured: Boolean = false,
    val gatewayTransactionId: String? = null,
    val message: String? = null,
)

class ManualPaymentGateway : PaymentGateway {
    override val name: String = "CASH_ON_DELIVERY"

    override suspend fun charge(
        orderId: String,
        amount: BigDecimal,
        paymentMethod: PaymentMethod,
        transactionId: String?,
    ): GatewayChargeResult =
        when (paymentMethod) {
            PaymentMethod.COD ->
                GatewayChargeResult(
                    success = true,
                    captured = false,
                    gatewayTransactionId = transactionId,
                    message = "Cash on delivery recorded; payment will be confirmed on collection",
                )
            else ->
                GatewayChargeResult(
                    success = false,
                    captured = false,
                    message = "No payment provider is configured for $paymentMethod. Use CASH_ON_DELIVERY or configure a gateway.",
                )
        }

    override suspend fun verifyWebhook(
        payload: String,
        signature: String?,
    ): Boolean = false
}
