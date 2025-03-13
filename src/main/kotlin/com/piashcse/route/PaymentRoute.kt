package com.piashcse.route

import com.piashcse.controller.PaymentController
import com.piashcse.models.AddPayment
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.paymentRoute(paymentController: PaymentController) {
    route("payment") {
        authenticate(RoleManagement.CUSTOMER.role) {
            post({
                tags("Payment")
                request {
                    body<AddPayment>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddPayment>()
                call.respond(
                    ApiResponse.success(
                        paymentController.addPayment(requestBody), HttpStatusCode.OK
                    )
                )
            }
            get("{id}", {
                tags("Payment")
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (id) = call.requiredParameters("id") ?: return@get
                call.respond(
                    ApiResponse.success(
                        paymentController.getPayment(
                            id
                        ), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}