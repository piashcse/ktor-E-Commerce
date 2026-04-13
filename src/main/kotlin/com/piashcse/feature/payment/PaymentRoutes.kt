package com.piashcse.feature.payment

import com.piashcse.model.request.PaymentRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.requireParameters
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
    route("/payment") {
        authenticate(RoleManagement.CUSTOMER.role) {
            /**
             * @tag Payment
             * @description Create a new payment record for an order
             * @operationId createPayment
             * @body PaymentRequest Payment creation request with payment details
             * @response 200 Payment created successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<PaymentRequest>()
                call.respond(
                    ApiResponse.ok(
                        paymentController.createPayment(requestBody)
                    )
                )
            }

            /**
             * @tag Payment
             * @description Retrieve payment details by payment ID
             * @operationId getPaymentById
             * @path id (required) Unique identifier of the payment
             * @response 200 Payment details retrieved successfully
             * @response 400 Invalid payment ID
             * @security jwtToken
             */
            get("{id}") {
                val id = call.requireParameters("id")
                call.respond(
                    ApiResponse.ok(
                        paymentController.getPaymentById(id.first())
                    )
                )
            }
        }
    }
}