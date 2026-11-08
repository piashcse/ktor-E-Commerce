package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.*
import com.piashcse.event.EventBus
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.PaymentCompletedEvent
import com.piashcse.mapper.toPaymentResponse
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
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
) : PaymentRepository {
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
        if (order.paymentStatus == PaymentStatus.COMPLETED) return
        val paid = completedTotal(order.id)
        if (paid < order.total) return

        order.paymentStatus = PaymentStatus.COMPLETED
        order.status = OrderStatus.PAID
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
        loadPayableOrder(paymentRequest, callerUserId, forUpdate = false)

        // Network call happens outside the DB transaction.
        val charge =
            gateway.charge(
                orderId = paymentRequest.orderId,
                amount = paymentRequest.amount,
                paymentMethod = paymentRequest.paymentMethod,
                transactionId = paymentRequest.transactionId,
            )
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

            if (payment.status == PaymentStatus.COMPLETED) {
                completeOrderIfFullyPaid(order, payment)
            }
            payment.toPaymentResponse()
        }
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
                    PaymentDAO.wrapRow(it).toPaymentResponse()
                }
        }
}
