package com.piashcse.feature.payment

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.*
import com.piashcse.mapper.toPaymentResponse
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.money.Money
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

class PaymentRepositoryImpl : PaymentRepository {

    override suspend fun getPaymentByTransactionId(transactionId: String): PaymentResponse? = query {
        PaymentDAO.find { PaymentTable.transactionId eq transactionId }.firstOrNull()?.toPaymentResponse()
    }

    override suspend fun getOrderForPayment(orderId: String): OrderPaymentInfo = query {
        val order = OrderDAO.find { OrderTable.id eq orderId.entityID(OrderTable) }.forUpdate().firstOrNull()
            ?: orderId.throwNotFound("Order")
        OrderPaymentInfo(orderId = order.id.value, userId = order.userId.value, orderTotal = order.total)
    }

    override suspend fun getCompletedPaymentsSum(orderId: String): BigDecimal = query {
        Money.round(
            PaymentDAO.find {
                (PaymentTable.orderId eq orderId.entityID(OrderTable)) and
                    (PaymentTable.status eq PaymentStatus.COMPLETED)
            }.toList().sumOf { it.amount },
        )
    }

    override suspend fun createPayment(
        orderId: String,
        userId: String,
        amount: BigDecimal,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        status: PaymentStatus,
    ): PaymentResponse = query {
        PaymentDAO.new {
            this.orderId = orderId.entityID(OrderTable)
            this.userId = userId.entityID(UserTable)
            this.amount = amount
            this.status = status
            this.paymentMethod = paymentMethod
            this.transactionId = transactionId
        }.toPaymentResponse()
    }

    override suspend fun finalizeOrderPayment(orderId: String) {
        query {
            val order = OrderDAO.findById(orderId) ?: return@query
            order.paymentStatus = PaymentStatus.COMPLETED
            order.status = OrderStatus.PAID
            StockReservationDAO.find { StockReservationTable.orderId eq orderId.entityID(OrderTable) }
                .forEach { it.status = ReservationStatus.FINALIZED }
        }
    }

    override suspend fun getUserEmail(userId: String): String? = query {
        UserDAO.findById(userId)?.email
    }

    override suspend fun getOrderOwnerId(orderId: String): String? = query {
        OrderDAO.findById(orderId)?.userId?.value
    }

    override suspend fun getPaymentById(paymentId: String): PaymentResponse =
        query {
            val isOrderExist = PaymentDAO.find { PaymentTable.id eq paymentId }.toList().firstOrNull()
            isOrderExist?.toPaymentResponse() ?: paymentId.throwNotFound("Payment")
        }

    override suspend fun getPaymentsByOrderId(
        orderId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PaymentResponse> =
        query {
            PaymentTable.selectAll().andWhere { PaymentTable.orderId eq orderId.entityID(OrderTable) }
                .orderBy(PaymentTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    PaymentDAO.wrapRow(it).toPaymentResponse()
                }
        }
}
