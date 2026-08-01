package com.piashcse.feature.payment

import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.PaginatedResponse

class PaymentService(private val paymentRepo: PaymentRepository) {
    suspend fun createPayment(paymentRequest: PaymentRequest): PaymentResponse = paymentRepo.createPayment(paymentRequest)

    suspend fun getPaymentById(paymentId: String): PaymentResponse = paymentRepo.getPaymentById(paymentId)

    suspend fun getPaymentsByOrderId(
        orderId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<PaymentResponse> = paymentRepo.getPaymentsByOrderId(orderId, limit, offset)
}
