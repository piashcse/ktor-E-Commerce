package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.*
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
import com.piashcse.utils.validator.ValidationException
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.slf4j.LoggerFactory
import java.util.UUID

/** Payment intent creation, risk checks, charge locking and confirmation. */
class PaymentIntentService(
    private val gateway: PaymentGateway,
    private val fraudService: FraudService = FraudService(),
    private val loyaltyService: LoyaltyService = LoyaltyService(),
    private val digitalDelivery: DigitalDeliveryService = DigitalDeliveryService(),
    private val gatewayBreaker: CircuitBreaker,
) {
    private val charges = PaymentChargeService(gateway, fraudService, gatewayBreaker)
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

        val authorized = charges.authorize(paymentRequest, callerUserId, ipAddress)
        return try {
            persistCharge(paymentRequest, callerUserId, authorized.risk, authorized.charge)
        } catch (e: Exception) {
            charges.compensate(paymentRequest, authorized.payable.currency, authorized.charge)
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

    private suspend fun persistCharge(
        paymentRequest: PaymentRequest,
        callerUserId: String,
        risk: RiskAssessment,
        charge: GatewayChargeResult,
    ): PaymentResponse =
        retryQuery {
            // Re-lock and re-validate: the order may have changed while the gateway ran.
            val snapshot = PaymentOrders.load(paymentRequest, callerUserId, forUpdate = true)
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

    suspend fun releaseReview(
        paymentId: String,
        actorUserId: String,
    ): PaymentResponse = confirmations.releaseReview(paymentId, actorUserId)

    suspend fun confirmPayment(
        paymentId: String,
        actorUserId: String?,
    ): PaymentResponse = confirmations.confirmPayment(paymentId, actorUserId)
}
