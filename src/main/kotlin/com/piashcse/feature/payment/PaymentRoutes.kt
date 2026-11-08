package com.piashcse.feature.payment

import com.piashcse.model.request.PaymentRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.idempotencyKey
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Customer payment routes.
 */
fun Route.paymentRoutes() {
    val paymentRepo: PaymentRepository by inject()
    customerOnlyAuth {
        writeRateLimit {
            /**
             * Create a payment intent for an order. Status is server-owned.
             *
             * @tag Payment
             * @description Create a payment intent for an order. Status is server-owned.
             */
            post {
                val body = call.receive<PaymentRequest>()
                val key = call.idempotencyKey(body.idempotencyKey)
                call.respondCreated(paymentRepo.createPayment(body.copy(idempotencyKey = key), call.currentUserId))
            }
        }

        /**
         * Retrieve payment details by ID
         *
         * @tag Payment
         * @description Retrieve payment details by ID
         */
        get("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(paymentRepo.getPaymentById(id, call.currentUserId))
        }

        /**
         * Retrieve all payments for a specific order
         *
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

/**
 * Admin payment confirmation routes. Used when cash is collected for a COD order
 * or when a provider webhook has been verified out-of-band.
 */
fun Route.paymentAdminRoutes() {
    val paymentRepo: PaymentRepository by inject()
    adminWriteRateLimit {
        /**
         * Admin: confirm a pending payment as completed
         *
         * @tag Payment
         * @description Admin: confirm a pending payment as completed
         */
        post("{id}/confirm") {
            val id = call.requirePathParameter("id")
            call.respondOk(paymentRepo.confirmPayment(id, call.currentUserId))
        }
    }
}
