package com.piashcse.feature.payment

import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider
import java.math.BigDecimal

class ManualPaymentGateway : PaymentGateway {
    override val name: String = "CASH_ON_DELIVERY"
    override val provider: PaymentProvider = PaymentProvider.MANUAL

    override fun supports(paymentMethod: PaymentMethod): Boolean = paymentMethod == PaymentMethod.COD

    override suspend fun charge(
        orderId: String,
        amount: BigDecimal,
        currency: String,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        returnUrl: String?,
        idempotencyKey: String?,
        require3ds: Boolean,
    ): GatewayChargeResult =
        if (paymentMethod == PaymentMethod.COD) {
            GatewayChargeResult(
                success = true,
                captured = false,
                gatewayTransactionId = transactionId,
                providerStatus = "manual",
                message = "Cash on delivery recorded; payment will be confirmed on collection",
            )
        } else {
            GatewayChargeResult(
                success = false,
                message = "No payment provider is configured for $paymentMethod. Use CASH_ON_DELIVERY or configure a gateway.",
            )
        }

    override suspend fun refund(
        providerReference: String,
        amount: BigDecimal,
        currency: String,
        idempotencyKey: String,
    ): GatewayRefundResult = GatewayRefundResult(false, message = "Manual payments must be refunded out-of-band")

    override fun verifyWebhook(
        payload: String,
        signature: String?,
    ): Boolean = false

    override fun parseWebhook(payload: String): GatewayWebhookEvent =
        GatewayWebhookEvent("manual", PaymentProvider.MANUAL, GatewayEventType.UNKNOWN)
}
