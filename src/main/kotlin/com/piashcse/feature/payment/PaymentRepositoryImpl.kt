package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RiskDecision
import com.piashcse.constants.ScaStatus
import com.piashcse.constants.WebhookEventStatus
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.EventBus
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.PaymentCompletedEvent
import com.piashcse.feature.fraud.FraudContext
import com.piashcse.feature.fraud.FraudService
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.mapper.toPaymentResponse
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.service.AppMetrics
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.resilience.CircuitBreaker
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.FraudBlockedException
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

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
) : PaymentRepository {
    private val gatewayBreaker = CircuitBreaker("payment-gateway", failureThreshold = 5, cooldownMs = 30_000)

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

    private fun completedTotal(orderId: EntityID<String>): BigDecimal =
        PaymentDAO.find {
            (PaymentTable.orderId eq orderId) and (PaymentTable.status eq PaymentStatus.COMPLETED)
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
        val paid = completedTotal(order.id)
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
        if (order.status == OrderStatus.CANCELED || order.status == OrderStatus.RECEIVED) {
            throw ValidationException(Message.Payments.CANNOT_PAY_CANCELED)
        }
        if (order.paymentStatus == PaymentStatus.COMPLETED || order.paymentStatus == PaymentStatus.REFUNDED) {
            throw ValidationException(Message.Payments.ALREADY_PAID)
        }

        val remaining = order.total.subtract(completedTotal(order.id))
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

        return retryQuery {
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

        return query {
            val existing =
                PaymentWebhookEventDAO.find {
                    (PaymentWebhookEventTable.provider eq event.provider) and
                        (PaymentWebhookEventTable.eventId eq event.eventId)
                }.firstOrNull()
            if (existing != null) {
                return@query WebhookResult(true, duplicate = true, message = "Duplicate webhook ignored")
            }

            val record =
                PaymentWebhookEventDAO.new {
                    this.provider = event.provider
                    this.eventId = event.eventId
                    this.eventType = event.type.name
                    this.payload = payload
                    this.status = WebhookEventStatus.RECEIVED
                }
            // Let unexpected failures roll back and surface as 5xx so the provider retries.
            // Only logically ignored events are acknowledged with 200.
            val applied = applyWebhookEvent(event)
            if (applied) {
                record.status = WebhookEventStatus.PROCESSED
                record.processedAt = currentUtc()
            } else if (record.status == WebhookEventStatus.RECEIVED) {
                record.status = WebhookEventStatus.IGNORED
                record.error = "No matching payment or refund for ${event.type}"
                record.processedAt = currentUtc()
            }
            WebhookResult(true, message = "Webhook processed")
        }
    }

    /** Must run inside the webhook transaction. */
    private fun applyWebhookEvent(event: GatewayWebhookEvent): Boolean =
        when (event.type) {
            GatewayEventType.PAYMENT_SUCCEEDED,
            GatewayEventType.PAYMENT_FAILED,
            GatewayEventType.PAYMENT_REQUIRES_ACTION,
            GatewayEventType.PAYMENT_PROCESSING,
            -> {
                val reference = event.providerReference ?: return false
                val payment =
                    PaymentDAO.find {
                        (PaymentTable.providerReference eq reference) or (PaymentTable.transactionId eq reference)
                    }.forUpdate().firstOrNull() ?: return false

                event.providerStatus?.let { payment.providerStatus = it }
                when (event.type) {
                    GatewayEventType.PAYMENT_SUCCEEDED -> {
                        payment.status = PaymentStatus.COMPLETED
                        payment.scaStatus = ScaStatus.AUTHENTICATED
                        OrderDAO.find { OrderTable.id eq payment.orderId }
                            .forUpdate()
                            .firstOrNull()
                            ?.let { completeOrderIfFullyPaid(it, payment) }
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
}
