package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RiskDecision
import com.piashcse.constants.ScaStatus
import com.piashcse.constants.WebhookEventStatus
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.AdminAudit
import com.piashcse.event.EventBus
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.PaymentCompletedEvent
import com.piashcse.feature.fraud.FraudContext
import com.piashcse.feature.fraud.FraudService
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.refund_request.RefundRequestRepository
import com.piashcse.feature.refund_request.RefundRequestRepositoryImpl
import com.piashcse.feature.seller.PayoutLedger
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.feature.subscription.SubscriptionBillingService
import com.piashcse.mapper.toPaymentResponse
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.service.AppMetrics
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.resilience.CircuitBreaker
import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.FraudBlockedException
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

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
    private val fraudService: FraudService = FraudService(),
    private val loyaltyService: LoyaltyService = LoyaltyService(),
    private val digitalDelivery: DigitalDeliveryService = DigitalDeliveryService(),
    private val subscriptionBilling: SubscriptionBillingService = SubscriptionBillingService(digitalDelivery),
    private val refunds: RefundRequestRepository = RefundRequestRepositoryImpl(),
) : PaymentRepository {
    private val log = LoggerFactory.getLogger(PaymentRepositoryImpl::class.java)
    private val gatewayBreaker = CircuitBreaker("payment-gateway", failureThreshold = 5, cooldownMs = 30_000)

    /**
     * Serializes payment creation per order inside this process. The database
     * re-validation still protects against cross-instance races; a distributed
     * lock/advisory lock is the next step for multi-instance deployments.
     */
    private val orderLocks = ConcurrentHashMap<String, Mutex>()
    private val orderLockRefs = ConcurrentHashMap<String, AtomicInteger>()

    private suspend fun <T> withOrderLock(
        orderId: String,
        block: suspend () -> T,
    ): T {
        val mutex = orderLocks.computeIfAbsent(orderId) { Mutex() }
        orderLockRefs.computeIfAbsent(orderId) { AtomicInteger() }.incrementAndGet()
        return try {
            mutex.withLock { block() }
        } finally {
            if (orderLockRefs.computeIfAbsent(orderId) { AtomicInteger() }.decrementAndGet() <= 0) {
                orderLocks.remove(orderId, mutex)
                orderLockRefs.remove(orderId)
            }
        }
    }

    private fun Throwable.isUniqueViolation(): Boolean =
        generateSequence(this) { it.cause }.any { t ->
            val msg = (t.message ?: "").lowercase()
            "duplicate" in msg || "unique" in msg || "23505" in msg
        }

    private fun PaymentDAO.isPayable(): Boolean = status in listOf(PaymentStatus.PENDING, PaymentStatus.FAILED)

    private fun findExistingByIdempotencyKey(
        key: String,
        callerUserId: String,
    ): PaymentDAO? =
        PaymentDAO.find {
            (PaymentTable.userId eq callerUserId.entityID(UserTable)) and
                (PaymentTable.idempotencyKey eq key)
        }.firstOrNull()

    private fun findPaymentByTxForCaller(
        txId: String,
        callerUserId: String,
    ): PaymentDAO? {
        val existing = PaymentDAO.find { PaymentTable.transactionId eq txId }.forUpdate().firstOrNull() ?: return null
        val existingOrder = OrderDAO.findById(existing.orderId.value)
        if (existingOrder == null || existingOrder.userId.value != callerUserId) {
            throw ForbiddenException(Message.Errors.FORBIDDEN)
        }
        return existing
    }

    private fun completedTotal(
        orderId: EntityID<String>,
        currency: String,
    ): BigDecimal =
        PaymentDAO.find {
            (PaymentTable.orderId eq orderId) and
                (PaymentTable.status eq PaymentStatus.COMPLETED) and
                (PaymentTable.currency eq currency)
        }.fold(BigDecimal.ZERO) { acc, payment -> acc.add(payment.amount) }

    /**
     * Marks the order PAID once completed payments cover its total. Must run inside
     * the transaction that locked the order row.
     */
    private fun completeOrderIfFullyPaid(
        order: OrderDAO,
        payment: PaymentDAO,
    ) {
        // REVIEW payments must be explicitly released by an admin before the order is paid.
        if (payment.riskDecision != RiskDecision.ALLOW) return
        if (order.paymentStatus == PaymentStatus.COMPLETED) return
        // Never resurrect an auto-canceled/received order. If the provider has
        // already captured money, the late payment needs an explicit refund.
        if (order.status == OrderStatus.CANCELED || order.status == OrderStatus.RECEIVED) return
        val paid = completedTotal(order.id, order.currency)
        if (paid < order.total) return

        order.paymentStatus = PaymentStatus.COMPLETED
        order.status = OrderStatus.PAID
        val loyaltyTotalUsd =
            if (order.fxRate > BigDecimal.ZERO) {
                order.total.divide(order.fxRate, 2, java.math.RoundingMode.HALF_UP)
            } else {
                order.total
            }
        loyaltyService.earnForPaidOrderTx(order.userId.value, order.id.value, loyaltyTotalUsd)
        digitalDelivery.grantForOrderTx(order)
        // Create the seller ledger inside the same transaction so an immediate
        // refund/cancellation can reverse it deterministically.
        PayoutLedger.createForOrderTx(order)
        StockReservationDAO.find { StockReservationTable.orderId eq order.id }
            .forEach { it.status = ReservationStatus.FINALIZED }
        OutboxPublisher.enqueueTx(
            "PAYMENT",
            payment.id.value,
            PaymentCompletedEvent(
                paymentId = payment.id.value,
                orderId = order.id.value,
                userId = order.userId.value,
                email = UserDAO.findById(order.userId.value)?.email.orEmpty(),
                amount = payment.amount,
            ),
        )
    }

    private data class PaymentOrderSnapshot(
        val orderId: EntityID<String>,
        val userId: EntityID<String>,
        val total: BigDecimal,
        val currency: String,
        val status: OrderStatus,
        val paymentStatus: PaymentStatus,
        val remaining: BigDecimal,
    )

    private fun loadPayableOrder(
        paymentRequest: PaymentRequest,
        callerUserId: String,
        forUpdate: Boolean,
    ): PaymentOrderSnapshot {
        val query =
            OrderDAO.find { OrderTable.id eq paymentRequest.orderId.entityID(OrderTable) }
        val order =
            (if (forUpdate) query.forUpdate() else query).firstOrNull()
                ?: paymentRequest.orderId.throwNotFound("Order")

        if (order.userId.value != callerUserId) throw ForbiddenException(Message.Errors.FORBIDDEN)
        if (order.status !in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)) {
            throw ValidationException("Order is not payable in status ${order.status}")
        }
        if (order.paymentStatus == PaymentStatus.COMPLETED || order.paymentStatus == PaymentStatus.REFUNDED) {
            throw ValidationException(Message.Payments.ALREADY_PAID)
        }

        val remaining = order.total.subtract(completedTotal(order.id, order.currency))
        if (remaining <= BigDecimal.ZERO) throw ValidationException(Message.Payments.ALREADY_PAID)
        if (paymentRequest.amount > remaining) {
            throw ValidationException(
                Message.Payments.amountExceedsRemaining(
                    paymentRequest.amount.toPlainString(),
                    remaining.toPlainString(),
                ),
            )
        }
        return PaymentOrderSnapshot(
            orderId = order.id,
            userId = order.userId,
            total = order.total,
            currency = order.currency,
            status = order.status,
            paymentStatus = order.paymentStatus,
            remaining = remaining,
        )
    }

    override suspend fun createPayment(
        paymentRequest: PaymentRequest,
        callerUserId: String,
        ipAddress: String?,
    ): PaymentResponse {
        val leaseOwner = UUID.randomUUID().toString()
        if (!acquireChargeLock(paymentRequest.orderId, leaseOwner)) {
            throw ConflictException("A payment for this order is already being processed")
        }
        return try {
            withOrderLock(paymentRequest.orderId) {
                createPaymentLocked(paymentRequest, callerUserId, ipAddress)
            }
        } finally {
            runCatching { releaseChargeLock(paymentRequest.orderId, leaseOwner) }
        }
    }

    private suspend fun acquireChargeLock(
        orderId: String,
        owner: String,
    ): Boolean {
        val existing = query { PaymentChargeLockDAO.find { PaymentChargeLockTable.orderId eq orderId.entityID(OrderTable) }.firstOrNull() }
        if (existing == null) {
            return try {
                query {
                    PaymentChargeLockDAO.new {
                        this.orderId = orderId.entityID(OrderTable)
                        this.owner = owner
                        this.lockedUntil = currentUtc().plusMinutes(5)
                    }
                }
                true
            } catch (e: Exception) {
                if (e.isUniqueViolation()) false else throw e
            }
        }
        if (existing.lockedUntil.isAfter(currentUtc())) return false
        return query {
            PaymentChargeLockTable.update(
                {
                    (PaymentChargeLockTable.orderId eq orderId.entityID(OrderTable)) and
                        (PaymentChargeLockTable.lockedUntil less currentUtc())
                },
            ) {
                it[PaymentChargeLockTable.owner] = owner
                it[PaymentChargeLockTable.lockedUntil] = currentUtc().plusMinutes(5)
            } > 0
        }
    }

    private suspend fun releaseChargeLock(
        orderId: String,
        owner: String,
    ) {
        query {
            PaymentChargeLockDAO.find {
                (PaymentChargeLockTable.orderId eq orderId.entityID(OrderTable)) and
                    (PaymentChargeLockTable.owner eq owner)
            }.forEach { it.delete() }
        }
    }

    @Suppress("LongMethod") // payment intent lifecycle kept in one atomic flow
    private suspend fun createPaymentLocked(
        paymentRequest: PaymentRequest,
        callerUserId: String,
        ipAddress: String?,
    ): PaymentResponse {
        val replay =
            query {
                paymentRequest.idempotencyKey?.let { key ->
                    findExistingByIdempotencyKey(key, callerUserId)?.let { existing ->
                        if (existing.orderId.value != paymentRequest.orderId) {
                            throw ValidationException("Idempotency key is already bound to another order")
                        }
                        return@query existing.toPaymentResponse()
                    }
                }
                paymentRequest.transactionId?.let { txId ->
                    findPaymentByTxForCaller(txId, callerUserId)?.let { existing ->
                        return@query existing.toPaymentResponse()
                    }
                }
                null
            }
        if (replay != null) return replay

        // Never leave two live provider intents for the same order: a second
        // confirmed intent would capture extra money that is never refunded.
        val openIntent =
            query {
                PaymentDAO.find {
                    (PaymentTable.orderId eq paymentRequest.orderId.entityID(OrderTable)) and
                        (PaymentTable.status eq PaymentStatus.PENDING) and
                        (PaymentTable.providerReference.isNotNull())
                }.firstOrNull()
            }
        if (openIntent != null) {
            if (openIntent.amount.compareTo(paymentRequest.amount) == 0) {
                return openIntent.toPaymentResponse()
            }
            throw ConflictException("A payment intent for this order is already open")
        }

        // Validate before charging so an invalid order never reaches the provider.
        val (payable, risk) =
            query {
                val snapshot = loadPayableOrder(paymentRequest, callerUserId, forUpdate = false)
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
                            ?: "charge:${paymentRequest.orderId}:${paymentRequest.amount.toPlainString()}:${paymentRequest.paymentMethod}",
                    require3ds = risk.requires3ds,
                )
            }
        if (!charge.success) {
            throw ValidationException(charge.message ?: Message.Payments.GATEWAY_FAILED)
        }

        return try {
            retryQuery {
                // Re-lock and re-validate: the order may have changed while the gateway ran.
                val snapshot = loadPayableOrder(paymentRequest, callerUserId, forUpdate = true)
                val order =
                    OrderDAO.find { OrderTable.id eq snapshot.orderId }.firstOrNull()
                        ?: paymentRequest.orderId.throwNotFound("Order")

                val payment =
                    try {
                        PaymentDAO.new {
                            this.orderId = order.id
                            this.userId = order.userId
                            this.amount = paymentRequest.amount
                            this.currency = order.currency
                            this.paymentMethod = paymentRequest.paymentMethod
                            this.transactionId = charge.gatewayTransactionId ?: paymentRequest.transactionId
                            this.idempotencyKey = paymentRequest.idempotencyKey
                            this.provider = gateway.provider
                            this.providerReference = charge.gatewayTransactionId
                            this.clientSecret = charge.clientSecret
                            this.nextActionUrl = charge.nextActionUrl
                            this.scaStatus = charge.scaStatus
                            this.providerStatus = charge.providerStatus
                            this.status = if (charge.captured) PaymentStatus.COMPLETED else PaymentStatus.PENDING
                        }
                    } catch (e: Exception) {
                        // Idempotency / transaction-id race: return the winner idempotently.
                        if (e.isUniqueViolation()) {
                            paymentRequest.idempotencyKey
                                ?.let { key -> findExistingByIdempotencyKey(key, callerUserId) }
                                ?.let { return@retryQuery it.toPaymentResponse() }
                            paymentRequest.transactionId
                                ?.let { tx -> findPaymentByTxForCaller(tx, callerUserId) }
                                ?.let { return@retryQuery it.toPaymentResponse() }
                        }
                        throw e
                    }
                fraudService.recordRisk(
                    payment,
                    risk.copy(avsStatus = charge.avsStatus, cvvStatus = charge.cvvStatus),
                )

                AppMetrics.paymentsCreated.increment()
                if (payment.status == PaymentStatus.COMPLETED) {
                    completeOrderIfFullyPaid(order, payment)
                }
                payment.toPaymentResponse()
            }
        } catch (e: Exception) {
            // The gateway has already captured money. If the post-charge
            // transaction fails (order changed, stock, DB race), compensating the
            // charge is the only way to avoid an orphan payment.
            val reference = charge.gatewayTransactionId
            if (charge.captured && reference != null) {
                runCatching {
                    gatewayBreaker.execute {
                        gateway.refund(
                            providerReference = reference,
                            amount = paymentRequest.amount,
                            currency = payable.currency,
                            idempotencyKey = "compensate:${paymentRequest.orderId}:$reference",
                        )
                    }
                }.onFailure {
                    log.error("Failed to compensate charge $reference for order ${paymentRequest.orderId}", it)
                }
            }
            throw e
        }
    }

    override suspend fun releaseReview(
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
                completeOrderIfFullyPaid(order, payment)
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

    override suspend fun confirmPayment(
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
            if (payment.status == PaymentStatus.FAILED && payment.provider != PaymentProvider.MANUAL) {
                throw ValidationException("A failed provider payment must be retried through the gateway")
            }

            val order =
                OrderDAO.find { OrderTable.id eq payment.orderId }
                    .forUpdate()
                    .firstOrNull()
                    ?: payment.orderId.value.throwNotFound("Order")

            if (order.status == OrderStatus.CANCELED || order.status == OrderStatus.RECEIVED) {
                throw ValidationException(Message.Payments.CANNOT_PAY_CANCELED)
            }

            payment.status = PaymentStatus.COMPLETED
            AppMetrics.paymentsConfirmed.increment()
            completeOrderIfFullyPaid(order, payment)

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

    override suspend fun handleWebhook(
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
                        payment.status = PaymentStatus.COMPLETED
                        payment.scaStatus = ScaStatus.AUTHENTICATED
                        completeOrderIfFullyPaid(order, payment)
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

    override suspend fun replayWebhook(
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

    override suspend fun getPaymentById(
        paymentId: String,
        callerUserId: String,
    ): PaymentResponse =
        query {
            val payment =
                PaymentDAO.find { PaymentTable.id eq paymentId.entityID(PaymentTable) }
                    .firstOrNull()
                    ?: paymentId.throwNotFound("Payment")
            val order = OrderDAO.findById(payment.orderId.value)
            if (order == null || order.userId.value != callerUserId) {
                throw ForbiddenException(Message.Errors.FORBIDDEN)
            }
            payment.toPaymentResponse()
        }

    override suspend fun getPaymentsByOrderId(
        orderId: String,
        callerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PaymentResponse> =
        query {
            val order =
                OrderDAO.findById(orderId)
                    ?: orderId.throwNotFound("Order")
            if (order.userId.value != callerUserId) {
                throw ForbiddenException(Message.Errors.FORBIDDEN)
            }
            PaymentTable.selectAll().andWhere { PaymentTable.orderId eq orderId.entityID(OrderTable) }
                .orderBy(PaymentTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    // Lists omit the client secret; it is only returned by create/get-by-id.
                    PaymentDAO.wrapRow(it).toPaymentResponse().copy(clientSecret = null)
                }
        }

    override suspend fun listPayments(
        status: PaymentStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PaymentResponse> =
        query {
            PaymentTable.selectAll()
                .also { query ->
                    status?.let { query.andWhere { PaymentTable.status eq it } }
                }.orderBy(PaymentTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    PaymentDAO.wrapRow(it).toPaymentResponse().copy(clientSecret = null)
                }
        }
}
