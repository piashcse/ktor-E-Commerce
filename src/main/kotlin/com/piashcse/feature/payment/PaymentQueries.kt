package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.PaymentTable
import com.piashcse.mapper.toPaymentResponse
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.readQuery
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ForbiddenException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Read-only payment lookups and listings. */
internal object PaymentQueries {
    suspend fun getPaymentById(
        paymentId: String,
        callerUserId: String,
    ): PaymentResponse =
        readQuery {
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

    suspend fun getPaymentsByOrderId(
        orderId: String,
        callerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PaymentResponse> =
        readQuery {
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

    suspend fun listPayments(
        status: PaymentStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PaymentResponse> =
        readQuery {
            PaymentTable.selectAll()
                .also { query ->
                    status?.let { query.andWhere { PaymentTable.status eq it } }
                }.orderBy(PaymentTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    PaymentDAO.wrapRow(it).toPaymentResponse().copy(clientSecret = null)
                }
        }
}
