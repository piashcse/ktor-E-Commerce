package com.piashcse.feature.payment

import com.piashcse.model.request.PaymentRequest
import com.piashcse.plugin.customerAuth
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Customer payment routes.
 */
fun Route.paymentRoutes(paymentController: PaymentService) {
    customerAuth {
        /**
         * @tag Payment
         * @description Create a new payment record for an order
         */
        post {
            val requestBody = call.receive<PaymentRequest>()
            call.respond(
                HttpStatusCode.OK,
                paymentController.createPayment(requestBody)
            )
        }

        /**
         * @tag Payment
         * @description Retrieve payment details by ID
         */
        get("{id}") {
            val id = call.requireParameters("id")
            call.respond(
                HttpStatusCode.OK,
                paymentController.getPaymentById(id.first())
            )
        }

        /**
         * @tag Payment
         * @description Retrieve all payments for a specific order
         */
        get("order/{orderId}") {
            val orderId = call.parameters["orderId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "orderId is required")
            val (limit, offset) = call.paginationParameters()
            call.respond(
                HttpStatusCode.OK,
                paymentController.getPaymentsByOrderId(orderId, limit, offset)
            )
        }
    }
}
