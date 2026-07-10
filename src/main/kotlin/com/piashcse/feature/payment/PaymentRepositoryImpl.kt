package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.PaymentTable
import com.piashcse.mapper.toPaymentResponse
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

class PaymentRepositoryImpl : PaymentRepository {
    override suspend fun createPayment(paymentRequest: PaymentRequest): PaymentResponse =
        query {
            val order =
                OrderDAO.findById(paymentRequest.orderId)
                    ?: paymentRequest.orderId.throwNotFound("Order")

            val orderTotal = order.total
            val paymentAmount = BigDecimal(paymentRequest.amount.toString())
            if (paymentAmount.compareTo(orderTotal) != 0) {
                throw ValidationException(Message.Payments.amountMismatch(paymentRequest.amount.toString(), orderTotal.toPlainString()))
            }

            val existingPayments =
                PaymentDAO.find {
                    (PaymentTable.orderId eq EntityID(paymentRequest.orderId, OrderTable)) and
                        (PaymentTable.status eq PaymentStatus.COMPLETED)
                }.toList()

            val paidAmount = existingPayments.sumOf { BigDecimal(it.amount.toString()) }
            if (paidAmount.compareTo(orderTotal) >= 0) {
                throw ValidationException(Message.Payments.ALREADY_PAID)
            }

            val payment =
                PaymentDAO.new {
                    this.orderId = EntityID(paymentRequest.orderId, OrderTable)
                    this.userId = order.userId
                    this.amount = paymentRequest.amount
                    this.status = paymentRequest.status
                    this.paymentMethod = paymentRequest.paymentMethod
                    this.transactionId = paymentRequest.transactionId
                }

            if (paidAmount.add(paymentAmount).compareTo(orderTotal) >= 0) {
                order.paymentStatus = PaymentStatus.COMPLETED
            }

            payment.toPaymentResponse()
        }

    override suspend fun getPaymentById(paymentId: String): PaymentResponse =
        query {
            val isOrderExist = PaymentDAO.find { PaymentTable.id eq paymentId }.toList().firstOrNull()
            isOrderExist?.toPaymentResponse() ?: paymentId.throwNotFound("PaymentResponse")
        }

    override suspend fun getPaymentsByOrderId(
        orderId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PaymentResponse> =
        query {
            PaymentTable.selectAll().andWhere { PaymentTable.orderId eq EntityID(orderId, OrderTable) }
                .orderBy(PaymentTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    PaymentDAO.wrapRow(it).toPaymentResponse()
                }
        }
}
