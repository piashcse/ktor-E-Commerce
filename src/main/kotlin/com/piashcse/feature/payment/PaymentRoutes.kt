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
    route("/payment") {
        authenticate(RoleManagement.CUSTOMER.role) {
            /**
             * @tag Payment
             * @body [PaymentRequest]
             * @response 200 [Response]
             */
            post {
                val requestBody = call.receive<PaymentRequest>()
                call.respond(
                    ApiResponse.success(
                        paymentController.createPayment(requestBody), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Payment
             * @path id (required)
             * @response 200 [Response]
             * @response 400
             */
            get("{id}") {
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