package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RiskDecision
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.EventBus
import com.piashcse.feature.fraud.FraudContext
import com.piashcse.feature.fraud.FraudService
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.refund_request.RefundRequestRepository
import com.piashcse.feature.refund_request.RefundRequestRepositoryImpl
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
import com.piashcse.utils.validator.ValidationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
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
    private val webhooks =
        PaymentWebhookService(
            gateway,
            refunds,
            subscriptionBilling,
            loyaltyService,
            digitalDelivery,
            gatewayBreaker,
        )

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

        val remaining = order.total.subtract(PaymentCompletion.completedTotal(order.id, order.currency))
        if (remaining <= BigDecimal.ZERO) throw ValidationException(Message.Payments.ALREADY_PAID)
        if (paymentRequest.amount > remaining) {
            throw ValidationException(
                Message.Payments.amountExceedsRemaining(
                    paymentRequest.amount.toPlainString(),
                    remaining.toPlainString(),
                ),
            )
        }
        // Partial card captures leave an order that can be neither cancelled
        // (a captured payment exists) nor refunded (refund requires delivery).
        // Until a deposit/partial-payment model exists, require settlement of
        // the full remaining amount.
        if (paymentRequest.amount.compareTo(remaining) != 0) {
            throw ValidationException(
                "Payment must settle the full remaining amount (${remaining.toPlainString()})",
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

        // Offline orders already have a server-created PENDING payment row.
        // Return it instead of creating a duplicate COD/bank-transfer row.
        val offlinePayment =
            query {
                PaymentDAO.find {
                    (PaymentTable.orderId eq paymentRequest.orderId.entityID(OrderTable)) and
                        (PaymentTable.userId eq callerUserId.entityID(UserTable)) and
                        (PaymentTable.status eq PaymentStatus.PENDING) and
                        (PaymentTable.provider eq PaymentProvider.MANUAL)
                }.firstOrNull()
            }
        if (offlinePayment != null) return offlinePayment.toPaymentResponse()

        // Never leave two live provider intents for the same order: a second
        // confirmed intent would capture extra money that is never refunded.
        val openIntent =
            query {
                PaymentDAO.find {
                    (PaymentTable.orderId eq paymentRequest.orderId.entityID(OrderTable)) and
                        // Never expose another caller's intent/client secret. The
                        // order-ownership check still runs below for non-replay paths.
                        (PaymentTable.userId eq callerUserId.entityID(UserTable)) and
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
                    // Namespace client keys per caller so one user cannot collide
                    // with another user's provider idempotency key.
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
                    PaymentCompletion.completeIfFullyPaid(order, payment, loyaltyService, digitalDelivery)
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
