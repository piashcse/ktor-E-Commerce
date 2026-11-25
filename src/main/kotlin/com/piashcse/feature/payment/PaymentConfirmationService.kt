package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RiskDecision
import com.piashcse.database.entities.*
import com.piashcse.event.EventBus
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.mapper.toPaymentResponse
import com.piashcse.model.response.PaymentResponse
import com.piashcse.service.AppMetrics
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq

/** Admin review release and manual payment confirmation. */
class PaymentConfirmationService(
    private val loyaltyService: LoyaltyService,
    private val digitalDelivery: DigitalDeliveryService,
) {
    suspend fun releaseReview(
        paymentId: String,
        actorUserId: String,
    ): PaymentResponse =
        retryQuery {
            val payment =
                PaymentDAO.find { PaymentTable.id eq paymentId.entityID(PaymentTable) }
                    .forUpdate()
                    .firstOrNull()
                    ?: paymentId.throwNotFound("Payment")
            if (payment.riskDecision == RiskDecision.BLOCK) {
                throw ValidationException("Blocked payments cannot be released")
            }
            payment.riskDecision = RiskDecision.ALLOW
            if (payment.status == PaymentStatus.COMPLETED) {
                val order =
                    OrderDAO.find { OrderTable.id eq payment.orderId }
                        .forUpdate()
                        .firstOrNull()
                        ?: payment.orderId.value.throwNotFound("Order")
                PaymentCompletion.completeIfFullyPaid(order, payment, loyaltyService, digitalDelivery)
            }
            val actor = runCatching { UserDAO.findById(actorUserId) }.getOrNull()
            EventBus.publishAdminAction(
                Triple(actorUserId, actor?.email.orEmpty(), actor?.userType?.name ?: "ADMIN"),
                "PAYMENT_REVIEW_RELEASED",
                "PAYMENT",
                paymentId,
                "Risk review released",
            )
            payment.toPaymentResponse()
        }

    suspend fun confirmPayment(
        paymentId: String,
        actorUserId: String?,
    ): PaymentResponse =
        retryQuery {
            val payment =
                PaymentDAO.find { PaymentTable.id eq paymentId.entityID(PaymentTable) }
                    .forUpdate()
                    .firstOrNull()
                    ?: paymentId.throwNotFound("Payment")

            if (payment.status == PaymentStatus.COMPLETED) return@retryQuery payment.toPaymentResponse()
            if (!payment.isPayable()) {
                throw ValidationException(Message.Payments.CANNOT_CONFIRM.format(payment.status))
            }
            if (payment.provider != PaymentProvider.MANUAL) {
                throw ValidationException(
                    "Provider payments must be confirmed by a verified gateway webhook",
                )
            }

            val order =
                OrderDAO.find { OrderTable.id eq payment.orderId }
                    .forUpdate()
                    .firstOrNull()
                    ?: payment.orderId.value.throwNotFound("Order")

            if (order.status == OrderStatus.CANCELED || order.status == OrderStatus.RECEIVED) {
                throw ValidationException(Message.Payments.CANNOT_PAY_CANCELED)
            }
            if (order.paymentStatus == PaymentStatus.REFUNDED) {
                throw ValidationException("Refunded orders cannot be paid again")
            }

            payment.status = PaymentStatus.COMPLETED
            AppMetrics.paymentsConfirmed.increment()
            PaymentCompletion.completeIfFullyPaid(order, payment, loyaltyService, digitalDelivery)

            if (actorUserId != null) {
                val actor = runCatching { UserDAO.findById(actorUserId) }.getOrNull()
                EventBus.publishAdminAction(
                    Triple(actorUserId, actor?.email.orEmpty(), actor?.userType?.name ?: "ADMIN"),
                    "PAYMENT_CONFIRMED",
                    "PAYMENT",
                    paymentId,
                    "Confirmed amount ${payment.amount.toPlainString()}",
                )
            }
            payment.toPaymentResponse()
        }
}

internal fun PaymentDAO.isPayable(): Boolean = status in listOf(PaymentStatus.PENDING, PaymentStatus.FAILED)
