package com.piashcse.feature.payment

import com.piashcse.model.request.PaymentRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.requiredParameters
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines routes for handling payments.
 *
 * Accessible by customers to create payments and retrieve payment details.
 *
 * @param paymentController The controller handling payment-related operations.
 */
fun Route.paymentRoutes(paymentController: PaymentService) {
    route("payment") {

        /**
         * POST request to create a new payment.
         *
         * @tag Payment
         * @summary auth[customer]
         * @body [PaymentRequest] The payment details (e.g., amount, payment method, etc.) to process the payment.
         * @response 200 [ApiResponse] Success response after creating payment
         */
        authenticate(RoleManagement.CUSTOMER.role) {
            post("/payment") {
                val requestBody = call.receive<PaymentRequest>()
                call.respond(
                    ApiResponse.success(
                        paymentController.createPayment(requestBody), HttpStatusCode.OK
                    )
                )
            }

            /**
             * GET request to retrieve payment details by ID.
             *
             * @tag Payment
             * @summary auth[customer]
             * @path id The payment ID to retrieve.
             * @response 200 [ApiResponse] Success response with payment details
             * @response 400 Bad request if id is missing
             */
            get("/payment/{id}") {
                val (id) = call.requiredParameters("id") ?: return@get
                call.respond(
                    ApiResponse.success(
                        paymentController.getPaymentById(id), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}