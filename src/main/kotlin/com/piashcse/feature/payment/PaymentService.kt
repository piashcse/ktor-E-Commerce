package com.piashcse.feature.payment

import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.PaymentTable
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.validator.ValidationException
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Service for managing payment-related operations.
 */
class PaymentService : PaymentRepository {
    /**
     * Creates a new payment for an order based on the provided payment details.
     *
     * @param paymentRequest The details of the payment including order ID, amount, status, and payment method.
     * @return The created payment entity.
     * @throws Exception if the order with the provided order ID is not found.
     */
    override suspend fun createPayment(paymentRequest: PaymentRequest): PaymentResponse =
        query {
            val order =
                OrderDAO.findById(paymentRequest.orderId)
                    ?: return@query paymentRequest.orderId.throwNotFound("Order")

            // Validate amount matches order total
            val orderTotal = order.total.toLong()
            if (paymentRequest.amount != orderTotal) {
                throw com.piashcse.utils.validator.ValidationException(
                    "PaymentResponse amount (${paymentRequest.amount}) does not match order total ($orderTotal)",
                )
            }

            // Check if already fully paid
            val existingPayments =
                PaymentDAO.find {
                    (PaymentTable.orderId eq EntityID(paymentRequest.orderId, OrderTable)) and
                        (PaymentTable.status eq PaymentStatus.COMPLETED)
                }.toList()

            val paidAmount = existingPayments.sumOf { it.amount }
            if (paidAmount >= orderTotal) {
                throw ValidationException("Order already fully paid")
            }

            // Create payment
            val payment =
                PaymentDAO.new {
                    this.orderId = EntityID(paymentRequest.orderId, OrderTable)
                    this.userId = order.userId
                    this.amount = paymentRequest.amount
                    this.status = paymentRequest.status
                    this.paymentMethod = paymentRequest.paymentMethod
                    this.transactionId = paymentRequest.transactionId
                }

            // Update order payment status if fully paid
            if (paidAmount + paymentRequest.amount >= orderTotal) {
                order.paymentStatus = PaymentStatus.COMPLETED
            }

            payment.response()
        }

    /**
     * Retrieves a payment by its ID.
     *
     * @param paymentId The ID of the payment to retrieve.
     * @return The payment entity associated with the provided payment ID.
     * @throws Exception if no payment is found for the given payment ID.
     */
    override suspend fun getPaymentById(paymentId: String): PaymentResponse =
        query {
            val isOrderExist = PaymentDAO.find { PaymentTable.id eq paymentId }.toList().firstOrNull()
            isOrderExist?.response() ?: paymentId.throwNotFound("PaymentResponse")
        }

    /**
     * Retrieves all payments for a specific order.
     *
     * @param orderId The ID of the order.
     * @return A list of payments for the order.
     */
    override suspend fun getPaymentsByOrderId(
        orderId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PaymentResponse> =
        query {
            PaymentTable.selectAll().andWhere { PaymentTable.orderId eq EntityID(orderId, PaymentTable) }
                .orderBy(PaymentTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    PaymentDAO.wrapRow(it).response()
                }
        }
}
