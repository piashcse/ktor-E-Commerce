package com.piashcse.feature.payment

import com.piashcse.model.request.PaymentRequest
import com.piashcse.plugin.RateLimitNames
import com.piashcse.plugin.customerAuth
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.idempotencyKey
import com.piashcse.utils.extension.respondOk
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Customer payment routes.
 */
fun Route.paymentRoutes() {
    val paymentRepo: PaymentRepository by inject()
    customerAuth {
        rateLimit(RateLimitName(RateLimitNames.WRITE)) {
            /**
             * @tag Payment
             * @description Create a new payment record for an order
             */
            post {
                val body = call.receive<PaymentRequest>()
                val txKey = call.idempotencyKey(body.transactionId)
                call.respondCreated(paymentRepo.createPayment(body.copy(transactionId = txKey), call.currentUserId))
            }
        }

        /**
         * @tag Payment
         * @description Retrieve payment details by ID
         */
        get("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(paymentRepo.getPaymentById(id, call.currentUserId))
        }

        /**
         * @tag Payment
         * @description Retrieve all payments for a specific order
         */
        get("order/{orderId}") {
            val orderId = call.requirePathParameter("orderId")
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(paymentRepo.getPaymentsByOrderId(orderId, call.currentUserId, limit, offset))
        }
    }
}
