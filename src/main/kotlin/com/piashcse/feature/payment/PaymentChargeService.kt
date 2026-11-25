package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.database.entities.*
import com.piashcse.feature.fraud.FraudContext
import com.piashcse.feature.fraud.FraudService
import com.piashcse.feature.fraud.RiskAssessment
import com.piashcse.model.request.PaymentRequest
import com.piashcse.service.payment.*
import com.piashcse.utils.extension.*
import com.piashcse.utils.resilience.CircuitBreaker
import com.piashcse.utils.validator.FraudBlockedException
import com.piashcse.utils.validator.ValidationException
import org.slf4j.LoggerFactory

/** Gateway charge authorization/risk checks and compensation refunds. */
internal class PaymentChargeService(
    private val gateway: PaymentGateway,
    private val fraudService: FraudService,
    private val gatewayBreaker: CircuitBreaker,
) {
    private val log = LoggerFactory.getLogger(PaymentChargeService::class.java)

    internal data class AuthorizedCharge(
        val payable: PaymentOrders.PaymentOrderSnapshot,
        val risk: RiskAssessment,
        val charge: GatewayChargeResult,
    )

    suspend fun authorize(
        paymentRequest: PaymentRequest,
        callerUserId: String,
        ipAddress: String?,
    ): AuthorizedCharge {
        // Validate before charging so an invalid order never reaches the provider.
        val (payable, risk) =
            query {
                val snapshot = PaymentOrders.load(paymentRequest, callerUserId, forUpdate = false)
                val email = UserDAO.findById(callerUserId)?.email
                snapshot to
                    fraudService.evaluate(
                        FraudContext(
                            userId = callerUserId,
                            email = email,
                            ipAddress = ipAddress,
                            amount = snapshot.remaining,
                        ),
                    )
            }
        if (risk.blocked) {
            throw FraudBlockedException(risk.reasons.firstOrNull() ?: "Payment blocked by fraud rules")
        }

        // Network call happens outside the DB transaction.
        val charge =
            gatewayBreaker.execute {
                gateway.charge(
                    orderId = paymentRequest.orderId,
                    amount = paymentRequest.amount,
                    currency = payable.currency,
                    paymentMethod = paymentRequest.paymentMethod,
                    transactionId = paymentRequest.transactionId,
                    returnUrl = paymentRequest.returnUrl,
                    idempotencyKey =
                        paymentRequest.idempotencyKey
                            ?.let { "u:$callerUserId:$it" }
                            ?: (
                                "charge:$callerUserId:${paymentRequest.orderId}:" +
                                    "${paymentRequest.amount.toPlainString()}:${paymentRequest.paymentMethod}"
                            ),
                    require3ds = risk.requires3ds,
                )
            }
        if (!charge.success) {
            throw ValidationException(charge.message ?: Message.Payments.GATEWAY_FAILED)
        }
        return AuthorizedCharge(payable, risk, charge)
    }

    suspend fun compensate(
        paymentRequest: PaymentRequest,
        currency: String,
        charge: GatewayChargeResult,
    ) {
        // The gateway has already captured money. If the post-charge transaction
        // fails, compensating the charge is the only way to avoid an orphan payment.
        val reference = charge.gatewayTransactionId ?: return
        if (!charge.captured) return
        runCatching {
            gatewayBreaker.execute {
                gateway.refund(
                    providerReference = reference,
                    amount = paymentRequest.amount,
                    currency = currency,
                    idempotencyKey = "compensate:${paymentRequest.orderId}:$reference",
                )
            }
        }.onFailure {
            log.error("Failed to compensate charge $reference for order ${paymentRequest.orderId}", it)
        }
    }
}
