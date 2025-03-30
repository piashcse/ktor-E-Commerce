package com.piashcse.controller

import com.piashcse.entities.*
import com.piashcse.models.PaymentRequest
import com.piashcse.repository.PaymentRepo
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID

/**
 * Controller for managing payment-related operations.
 */
class PaymentController : PaymentRepo {

    /**
     * Creates a new payment for an order based on the provided payment details.
     *
     * @param paymentRequest The details of the payment including order ID, amount, status, and payment method.
     * @return The created payment entity.
     * @throws Exception if the order with the provided order ID is not found.
     */
    override suspend fun createPayment(paymentRequest: PaymentRequest): Payment = query {
        val isOrderExist = OrderEntity.find { OrderTable.id eq paymentRequest.orderId }.toList().singleOrNull()
        isOrderExist?.let {
            PaymentEntity.new {
                orderId = EntityID(paymentRequest.orderId, PaymentTable)
                amount = paymentRequest.amount
                status = paymentRequest.status
                paymentMethod = paymentRequest.paymentMethod
            }.response()
        } ?: throw paymentRequest.orderId.notFoundException()
    }

    /**
     * Retrieves a payment by its ID.
     *
     * @param paymentId The ID of the payment to retrieve.
     * @return The payment entity associated with the provided payment ID.
     * @throws Exception if no payment is found for the given payment ID.
     */
    override suspend fun getPaymentById(paymentId: String): Payment = query {
        val isOrderExist = PaymentEntity.find { PaymentTable.id eq paymentId }.toList().firstOrNull()
        isOrderExist?.response() ?: throw paymentId.notFoundException()
    }
}