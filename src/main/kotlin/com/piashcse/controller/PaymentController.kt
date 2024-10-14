package com.piashcse.controller

import com.piashcse.entities.Payment
import com.piashcse.entities.PaymentEntity
import com.piashcse.entities.PaymentTable
import com.piashcse.entities.OrderEntity
import com.piashcse.entities.OrderTable
import com.piashcse.models.AddPayment
import com.piashcse.repository.PaymentRepo
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID

class PaymentController : PaymentRepo {
    override suspend fun addPayment(payment: AddPayment): Payment = query {
        val isOrderExist = OrderEntity.find { OrderTable.id eq payment.orderId }.toList().singleOrNull()
        isOrderExist?.let {
            PaymentEntity.new {
                orderId = EntityID(payment.orderId, PaymentTable)
                amount = payment.amount
                status = payment.status
                paymentMethod = payment.paymentMethod
            }.response()
        } ?: throw payment.orderId.notFoundException()

    }

    override suspend fun getPayment(paymentId: String): Payment = query {
        val isOrderExist = PaymentEntity.find { PaymentTable.id eq paymentId }.toList().firstOrNull()
        isOrderExist?.response() ?: throw paymentId.notFoundException()
    }
}