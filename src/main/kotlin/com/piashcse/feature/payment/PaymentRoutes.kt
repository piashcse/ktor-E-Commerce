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
 * Admins can process refunds.
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
                    ApiResponse.success(
                        paymentController.createPayment(requestBody), HttpStatusCode.OK
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
                val (id) = call.requiredParameters("id") ?: return@get
                call.respond(
                    ApiResponse.success(
                        paymentController.getPaymentById(id), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Payment
             * @description Retrieve payment by order ID
             * @operationId getPaymentByOrderId
             * @query orderId (required) Unique identifier of the order
             * @response 200 Payment details retrieved successfully
             * @security jwtToken
             */
            get("order/{orderId}") {
                val (orderId) = call.requiredParameters("orderId") ?: return@get
                val payment = paymentController.getPaymentByOrderId(orderId)
                if (payment != null) {
                    call.respond(ApiResponse.success(payment, HttpStatusCode.OK))
                } else {
                    call.respond(ApiResponse.success("No payment found for order $orderId", HttpStatusCode.OK))
                }
            }
        }

        authenticate(RoleManagement.ADMIN.role, RoleManagement.SUPER_ADMIN.role) {
            /**
             * @tag Payment
             * @description Admin: Mark a payment as paid
             * @operationId markPaymentAsPaid
             * @path paymentId (required) Unique identifier of the payment
             * @query transactionId (optional) Transaction ID from payment gateway
             * @response 200 Payment marked as paid
             * @security jwtToken
             */
            patch("{paymentId}/mark-paid") {
                val (paymentId) = call.requiredParameters("paymentId") ?: return@patch
                val transactionId = call.parameters["transactionId"]
                call.respond(
                    ApiResponse.success(
                        paymentController.markAsPaid(paymentId, transactionId), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Payment
             * @description Admin: Process a refund for a payment
             * @operationId processRefund
             * @path paymentId (required) Unique identifier of the payment
             * @query refundAmount (optional) Amount to refund (defaults to full amount)
             * @query reason (optional) Reason for refund
             * @response 200 Refund processed successfully
             * @security jwtToken
             */
            patch("{paymentId}/refund") {
                val (paymentId) = call.requiredParameters("paymentId") ?: return@patch
                val refundAmount = call.parameters["refundAmount"]?.toDoubleOrNull()
                val reason = call.parameters["reason"]
                call.respond(
                    ApiResponse.success(
                        paymentController.processRefund(paymentId, refundAmount, reason), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}