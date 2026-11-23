package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.*
import com.piashcse.feature.fraud.FraudContext
import com.piashcse.feature.fraud.FraudService
import com.piashcse.feature.fraud.RiskAssessment
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.mapper.toPaymentResponse
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.service.AppMetrics
import com.piashcse.utils.db.isUniqueViolation
import com.piashcse.utils.extension.*
import com.piashcse.utils.resilience.CircuitBreaker
import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.FraudBlockedException
import com.piashcse.utils.validator.ValidationException
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.UUID

/** Payment intent creation, risk checks, charge locking and confirmation. */
class PaymentIntentService(
    private val gateway: PaymentGateway,
    private val fraudService: FraudService = FraudService(),
    private val loyaltyService: LoyaltyService = LoyaltyService(),
    private val digitalDelivery: DigitalDeliveryService = DigitalDeliveryService(),
    private val gatewayBreaker: CircuitBreaker,
) {
    private val confirmations = PaymentConfirmationService(loyaltyService, digitalDelivery)
    private val log = LoggerFactory.getLogger(PaymentIntentService::class.java)

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

    suspend fun createPayment(
        paymentRequest: PaymentRequest,
        callerUserId: String,
        ipAddress: String?,
    ): PaymentResponse {
        val leaseOwner = UUID.randomUUID().toString()
        if (!PaymentChargeLocks.acquire(paymentRequest.orderId, leaseOwner)) {
            throw ConflictException("A payment for this order is already being processed")
        }
        return try {
            PaymentChargeLocks.withLock(paymentRequest.orderId) {
                createPaymentLocked(paymentRequest, callerUserId, ipAddress)
            }
        } finally {
            runCatching { PaymentChargeLocks.release(paymentRequest.orderId, leaseOwner) }
        }
    }

    suspend fun createPaymentLocked(
        paymentRequest: PaymentRequest,
        callerUserId: String,
        ipAddress: String?,
    ): PaymentResponse {
        findReplay(paymentRequest, callerUserId)?.let { return it }
        findExistingIntent(paymentRequest, callerUserId)?.let { return it }

        val authorized = authorizeCharge(paymentRequest, callerUserId, ipAddress)
        return try {
            persistCharge(paymentRequest, callerUserId, authorized.risk, authorized.charge)
        } catch (e: Exception) {
            compensateCharge(paymentRequest, authorized.payable.currency, authorized.charge)
            throw e
        }
    }

    private suspend fun findReplay(
        paymentRequest: PaymentRequest,
        callerUserId: String,
    ): PaymentResponse? =
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

    private suspend fun findExistingIntent(
        paymentRequest: PaymentRequest,
        callerUserId: String,
    ): PaymentResponse? {
        // Offline orders already have a server-created PENDING payment row.
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

        // Never leave two live provider intents for the same order.
        val openIntent =
            query {
                PaymentDAO.find {
                    (PaymentTable.orderId eq paymentRequest.orderId.entityID(OrderTable)) and
                        (PaymentTable.userId eq callerUserId.entityID(UserTable)) and
                        (PaymentTable.status eq PaymentStatus.PENDING) and
                        (PaymentTable.providerReference.isNotNull())
                }.firstOrNull()
            }
        if (openIntent == null) return null
        if (openIntent.amount.compareTo(paymentRequest.amount) == 0) return openIntent.toPaymentResponse()
        throw ConflictException("A payment intent for this order is already open")
    }

    private data class AuthorizedCharge(
        val payable: PaymentOrderSnapshot,
        val risk: RiskAssessment,
        val charge: GatewayChargeResult,
    )

    private suspend fun authorizeCharge(
        paymentRequest: PaymentRequest,
        callerUserId: String,
        ipAddress: String?,
    ): AuthorizedCharge {
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

    private suspend fun persistCharge(
        paymentRequest: PaymentRequest,
        callerUserId: String,
        risk: RiskAssessment,
        charge: GatewayChargeResult,
    ): PaymentResponse =
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

    private suspend fun compensateCharge(
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

    suspend fun releaseReview(
        paymentId: String,
        actorUserId: String,
    ): PaymentResponse = confirmations.releaseReview(paymentId, actorUserId)

    suspend fun confirmPayment(
        paymentId: String,
        actorUserId: String?,
    ): PaymentResponse = confirmations.confirmPayment(paymentId, actorUserId)
}
