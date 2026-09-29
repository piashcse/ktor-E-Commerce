package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.*
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.PaymentCompletedEvent
import com.piashcse.mapper.toPaymentResponse
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class PaymentRepositoryImpl : PaymentRepository {
    override suspend fun createPayment(paymentRequest: PaymentRequest, callerUserId: String): PaymentResponse {
        val (response, _) = retryQuery {
            paymentRequest.transactionId?.let { txId ->
                PaymentDAO.find { PaymentTable.transactionId eq txId }.firstOrNull()
                    ?.let { existing ->
                        // Idempotency: only return existing payment if caller owns the order.
                        val existingOrder = OrderDAO.findById(existing.orderId.value)
                        if (existingOrder == null || existingOrder.userId.value != callerUserId) {
                            throw ValidationException(Message.Errors.FORBIDDEN)
                        }
                        return@retryQuery Pair(existing.toPaymentResponse(), null)
                    }
            }

            val order =
                OrderDAO.find { OrderTable.id eq paymentRequest.orderId.entityID(OrderTable) }.forUpdate().firstOrNull()
                    ?: paymentRequest.orderId.throwNotFound("Order")

            if (order.userId.value != callerUserId) {
                throw ValidationException(Message.Errors.FORBIDDEN)
            }

            val orderTotal = order.total
            val paymentAmount = paymentRequest.amount
            if (paymentAmount.compareTo(orderTotal) != 0) {
                throw ValidationException(Message.Payments.amountMismatch(paymentRequest.amount.toPlainString(), orderTotal.toPlainString()))
            }

            val existingPayments =
                PaymentDAO.find {
                    (PaymentTable.orderId eq paymentRequest.orderId.entityID(OrderTable)) and
                        (PaymentTable.status eq PaymentStatus.COMPLETED)
                }.toList()

            val paidAmount = existingPayments.sumOf { it.amount }
            if (paidAmount.compareTo(orderTotal) >= 0) {
                throw ValidationException(Message.Payments.ALREADY_PAID)
            }

            val payment =
                PaymentDAO.new {
                    this.orderId = paymentRequest.orderId.entityID(OrderTable)
                    this.userId = order.userId
                    this.amount = paymentRequest.amount
                    this.status = paymentRequest.status
                    this.paymentMethod = paymentRequest.paymentMethod
                    this.transactionId = paymentRequest.transactionId
                }

            if (paidAmount.add(paymentAmount).compareTo(orderTotal) >= 0) {
                order.paymentStatus = PaymentStatus.COMPLETED
                StockReservationDAO.find { StockReservationTable.orderId eq paymentRequest.orderId.entityID(OrderTable) }
                    .forEach { it.status = ReservationStatus.FINALIZED }
                OutboxPublisher.enqueueTx(
                    "PAYMENT", payment.id.value,
                    PaymentCompletedEvent(
                        paymentId = payment.id.value,
                        orderId = paymentRequest.orderId,
                        userId = order.userId.value,
                        email = UserDAO.findById(order.userId.value)?.email.orEmpty(),
                        amount = paymentAmount,
                    ),
                )
            }

            Pair(payment.toPaymentResponse(), null)
        }
        return response
    }

    override suspend fun getPaymentById(paymentId: String, callerUserId: String): PaymentResponse =
        query {
            val payment = PaymentDAO.find { PaymentTable.id eq paymentId }.toList().firstOrNull()
                ?: paymentId.throwNotFound("PaymentResponse")
            val order = OrderDAO.findById(payment.orderId.value)
            if (order == null || order.userId.value != callerUserId) {
                throw ValidationException(Message.Errors.FORBIDDEN)
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
            val order = OrderDAO.findById(orderId)
                ?: orderId.throwNotFound("Order")
            if (order.userId.value != callerUserId) {
                throw ValidationException(Message.Errors.FORBIDDEN)
            }
            PaymentTable.selectAll().andWhere { PaymentTable.orderId eq orderId.entityID(OrderTable) }
                .orderBy(PaymentTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    PaymentDAO.wrapRow(it).toPaymentResponse()
                }
        }
}
