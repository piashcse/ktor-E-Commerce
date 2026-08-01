package com.piashcse.feature.payment

import com.piashcse.model.request.PaymentRequest
import com.piashcse.plugin.RateLimitNames
import com.piashcse.plugin.customerAuth
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.requireUserType
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Customer payment routes.
 */
fun Route.paymentRoutes() {
    val paymentService: PaymentService by inject()
    customerAuth {
        rateLimit(RateLimitName(RateLimitNames.WRITE)) {
            /**
             * @tag Payment
             * @description Create a new payment record for the caller's order
             */
            post {
                call.respondCreated(paymentService.createPayment(call.currentUserId, call.receive<PaymentRequest>()))
            }
        }

        /**
         * @tag Payment
         * @description Retrieve payment details by ID (order owner or admin)
         */
        get("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(paymentService.getPaymentById(call.currentUserId, call.requireUserType(), id))
        }

        /**
         * @tag Payment
         * @description Retrieve all payments for a specific order (order owner or admin)
         */
        get("order/{orderId}") {
            val orderId = call.requirePathParameter("orderId")
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(
                paymentService.getPaymentsByOrderId(
                    call.currentUserId,
                    call.requireUserType(),
                    orderId,
                    limit,
                    offset,
                ),
            )
        }
    }
}
