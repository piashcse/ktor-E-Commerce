package com.piashcse.feature.payment

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.ScaStatus
import com.piashcse.constants.WebhookEventStatus
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.AdminAudit
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.refund_request.RefundRequestRepository
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.feature.subscription.SubscriptionBillingService
import com.piashcse.service.AppMetrics
import com.piashcse.utils.extension.*
import com.piashcse.utils.resilience.CircuitBreaker
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or

/** Payment provider webhook verification, state reconciliation and replay. */
class PaymentWebhookService(
    private val gateway: PaymentGateway,
    private val refunds: RefundRequestRepository,
    private val subscriptionBilling: SubscriptionBillingService,
    private val loyaltyService: LoyaltyService,
    private val digitalDelivery: DigitalDeliveryService,
    private val gatewayBreaker: CircuitBreaker,
) {
    suspend fun handleWebhook(
        provider: String,
        payload: String,
        signature: String?,
    ): WebhookResult {
        if (!gateway.provider.name.equals(provider, ignoreCase = true)) {
            throw NotFoundException("Unknown payment provider: $provider")
        }
        if (!gateway.verifyWebhook(payload, signature)) {
            throw ValidationException("Invalid webhook signature")
        }
        val event =
            runCatching { gateway.parseWebhook(payload) }
                .getOrElse { throw ValidationException("Invalid webhook payload") }
        AppMetrics.paymentWebhooks.increment()

        if (event.type == GatewayEventType.REFUND_UPDATED && event.refund != null) {
            val refund = event.refund
            val existing =
                query {
                    PaymentWebhookEventDAO.find {
                        (PaymentWebhookEventTable.provider eq event.provider) and
                            (PaymentWebhookEventTable.eventId eq event.eventId)
                    }.firstOrNull()
                }
            if (existing != null && existing.status in listOf(WebhookEventStatus.PROCESSED, WebhookEventStatus.IGNORED)) {
                return WebhookResult(true, duplicate = true, message = "Duplicate webhook ignored")
            }
            // Reconcile before committing PROCESSED: a failure must stay
            // retryable instead of being acknowledged and lost.
            val reconciled =
                refunds.reconcileProviderRefund(refund.providerRefundId, refund.status, refund.amount)
            query {
                val record =
                    existing ?: PaymentWebhookEventDAO.new {
                        this.provider = event.provider
                        this.eventId = event.eventId
                        this.eventType = event.type.name
                        this.payload = payload
                        this.status = WebhookEventStatus.RECEIVED
                    }
                record.status = if (reconciled) WebhookEventStatus.PROCESSED else WebhookEventStatus.IGNORED
                record.error = if (reconciled) null else "No matching local refund request"
                record.processedAt = currentUtc()
            }
            return WebhookResult(true, message = if (reconciled) "Refund update reconciled" else "Refund update ignored")
        }

        val outcome =
            query {
                val existing =
                    PaymentWebhookEventDAO.find {
                        (PaymentWebhookEventTable.provider eq event.provider) and
                            (PaymentWebhookEventTable.eventId eq event.eventId)
                    }.firstOrNull()
                if (existing != null && existing.status in listOf(WebhookEventStatus.PROCESSED, WebhookEventStatus.IGNORED)) {
                    return@query Pair(WebhookResult(true, duplicate = true, message = "Duplicate webhook ignored"), null)
                }

                // A retry of an unprocessed delivery reuses the stored row.
                val record =
                    existing ?: PaymentWebhookEventDAO.new {
                        this.provider = event.provider
                        this.eventId = event.eventId
                        this.eventType = event.type.name
                        this.payload = payload
                        this.status = WebhookEventStatus.RECEIVED
                    }
                record.status = WebhookEventStatus.RECEIVED
                record.error = null
                // true = applied, false = logically ignored, null = retry later
                // (most commonly a webhook that arrived before the payment row).
                when (applyWebhookEvent(event)) {
                    true -> {
                        record.status = WebhookEventStatus.PROCESSED
                        record.processedAt = currentUtc()
                        Pair(WebhookResult(true, message = "Webhook processed"), null)
                    }
                    null -> {
                        record.status = WebhookEventStatus.RECEIVED
                        record.error = "Payment/refund not found yet; provider should retry"
                        // A captured payment for an order that was auto-canceled
                        // must be refunded, not applied to the canceled order.
                        Pair(
                            WebhookResult(false, message = "Webhook queued for retry"),
                            event.takeIf { it.type == GatewayEventType.PAYMENT_SUCCEEDED },
                        )
                    }
                    false -> {
                        record.status = WebhookEventStatus.IGNORED
                        record.error = "No matching payment or refund for ${event.type}"
                        record.processedAt = currentUtc()
                        Pair(WebhookResult(true, message = "Webhook ignored"), null)
                    }
                }
            }

        // Gateway calls must run outside the transaction; if the late payment
        // was refunded, mark the stored event processed.
        val retryEvent = outcome.second
        if (retryEvent != null && refundLatePayment(retryEvent)) {
            query {
                PaymentWebhookEventDAO.find {
                    (PaymentWebhookEventTable.provider eq event.provider) and
                        (PaymentWebhookEventTable.eventId eq event.eventId)
                }.firstOrNull()?.apply {
                    status = WebhookEventStatus.PROCESSED
                    error = "Refunded payment for a canceled order"
                    processedAt = currentUtc()
                }
            }
            return WebhookResult(true, message = "Late payment refunded")
        }
        return outcome.first
    }

    /** Refunds a captured payment whose order is no longer payable. Must run outside a transaction. */
    private suspend fun refundLatePayment(event: GatewayWebhookEvent): Boolean {
        val reference = event.providerReference ?: return false
        val payment =
            query {
                PaymentDAO.find {
                    (PaymentTable.providerReference eq reference) or (PaymentTable.transactionId eq reference)
                }.firstOrNull { it.status != PaymentStatus.REFUNDED }
            } ?: return false
        // Only refund a capture whose order is genuinely no longer payable.
        val order = OrderDAO.findById(payment.orderId.value) ?: return false
        if (order.status != OrderStatus.CANCELED && order.status != OrderStatus.RECEIVED) return false
        val providerReference = payment.providerReference ?: return false
        val refund =
            runCatching {
                gatewayBreaker.execute {
                    gateway.refund(
                        providerReference = providerReference,
                        amount = payment.amount,
                        currency = payment.currency,
                        idempotencyKey = "late-cancel:${payment.id.value}",
                    )
                }
            }.getOrNull() ?: return false
        if (!refund.success) return false
        query {
            val row = PaymentDAO.findById(payment.id.value) ?: return@query
            row.status = PaymentStatus.REFUNDED
            row.providerStatus = "paid_on_canceled_order_refunded"
        }
        return true
    }

    /** Must run inside the webhook transaction. Returns null when the event should be retried. */
    @Suppress("CyclomaticComplexMethod") // payment state machine intentionally exhaustive
    private fun applyWebhookEvent(event: GatewayWebhookEvent): Boolean? {
        // Subscription renewals are PaymentIntents without an order; route them by invoice metadata.
        val subscriptionInvoiceId = event.metadata["subscription_invoice_id"]
        if (!subscriptionInvoiceId.isNullOrBlank()) {
            return when (event.type) {
                GatewayEventType.PAYMENT_SUCCEEDED ->
                    subscriptionBilling.applyPaidInvoiceTx(
                        subscriptionInvoiceId,
                        event.providerReference,
                        event.amount,
                        event.currency,
                    )
                GatewayEventType.PAYMENT_FAILED ->
                    subscriptionBilling.applyFailedInvoiceTx(
                        subscriptionInvoiceId,
                        event.providerStatus ?: "Subscription payment failed at provider",
                    )
                else -> false
            }
        }
        return when (event.type) {
            GatewayEventType.PAYMENT_SUCCEEDED,
            GatewayEventType.PAYMENT_FAILED,
            GatewayEventType.PAYMENT_REQUIRES_ACTION,
            GatewayEventType.PAYMENT_PROCESSING,
            -> {
                val reference = event.providerReference ?: return false
                val payment =
                    PaymentDAO.find {
                        (PaymentTable.providerReference eq reference) or (PaymentTable.transactionId eq reference)
                    }.forUpdate().firstOrNull() ?: return null

                // Never let a late webhook rewrite money that already settled or
                // was refunded, and reject events that do not match the payment.
                if (event.amount != null && event.amount.compareTo(payment.amount) != 0) return false
                if (event.currency != null && !event.currency.equals(payment.currency, ignoreCase = true)) return false
                if (payment.status == PaymentStatus.REFUNDED) return true
                if (payment.status == PaymentStatus.COMPLETED && event.type == GatewayEventType.PAYMENT_FAILED) {
                    event.providerStatus?.let { payment.providerStatus = it }
                    return true
                }

                event.providerStatus?.let { payment.providerStatus = it }
                when (event.type) {
                    GatewayEventType.PAYMENT_SUCCEEDED -> {
                        val order =
                            OrderDAO.find { OrderTable.id eq payment.orderId }
                                .forUpdate()
                                .firstOrNull()
                        if (order == null) return null
                        // The order may have been auto-canceled while an async
                        // payment was in flight. Do not resurrect it; leave the
                        // event retryable so it can be refunded/reconciled.
                        if (order.status == OrderStatus.CANCELED || order.status == OrderStatus.RECEIVED) {
                            payment.providerStatus = "paid_on_canceled_order"
                            return null
                        }
                        if (order.paymentStatus == PaymentStatus.REFUNDED) {
                            payment.providerStatus = "paid_on_refunded_order"
                            return true
                        }
                        payment.status = PaymentStatus.COMPLETED
                        payment.scaStatus = ScaStatus.AUTHENTICATED
                        PaymentCompletion.completeIfFullyPaid(order, payment, loyaltyService, digitalDelivery)
                    }
                    GatewayEventType.PAYMENT_FAILED -> {
                        payment.status = PaymentStatus.FAILED
                        payment.scaStatus = ScaStatus.FAILED
                    }
                    GatewayEventType.PAYMENT_REQUIRES_ACTION -> {
                        payment.scaStatus = ScaStatus.REQUIRED
                    }
                    GatewayEventType.PAYMENT_PROCESSING -> {
                        payment.scaStatus = ScaStatus.PENDING
                    }
                    else -> Unit
                }
                true
            }
            GatewayEventType.REFUND_UPDATED -> {
                val refund = event.refund ?: return false
                val request =
                    RefundRequestDAO.find { RefundRequestTable.providerRefundId eq refund.providerRefundId }
                        .forUpdate()
                        .firstOrNull() ?: return false
                request.providerRefundStatus = refund.status
                true
            }
            GatewayEventType.UNKNOWN -> false
        }
    }

    suspend fun replayWebhook(
        eventId: String,
        actorUserId: String,
    ): WebhookResult =
        query {
            val record =
                PaymentWebhookEventDAO.find {
                    PaymentWebhookEventTable.id eq eventId.entityID(PaymentWebhookEventTable)
                }.forUpdate().firstOrNull() ?: eventId.throwNotFound("Webhook event")
            val event =
                runCatching { gateway.parseWebhook(record.payload) }
                    .getOrElse { error ->
                        record.status = WebhookEventStatus.FAILED
                        record.error = error.message ?: "Invalid stored webhook payload"
                        record.processedAt = currentUtc()
                        AdminAudit.publish(
                            actorUserId,
                            "WEBHOOK_REPLAY_FAILED",
                            "PAYMENT_WEBHOOK",
                            eventId,
                            record.error,
                        )
                        return@query WebhookResult(false, message = "Invalid stored webhook payload")
                    }
            val applied = applyWebhookEvent(event)
            record.status =
                when (applied) {
                    true -> WebhookEventStatus.PROCESSED
                    null -> WebhookEventStatus.RECEIVED
                    false -> WebhookEventStatus.IGNORED
                }
            record.error =
                when (applied) {
                    true -> null
                    null -> "No matching payment/refund yet; provider should retry"
                    false -> "No matching payment/refund for ${event.type}"
                }
            record.processedAt = if (applied == null) null else currentUtc()
            AdminAudit.publish(
                actorUserId,
                "WEBHOOK_REPLAYED",
                "PAYMENT_WEBHOOK",
                eventId,
                "${event.type}: ${record.status}",
            )
            WebhookResult(applied != null, message = "Webhook replayed")
        }
}
