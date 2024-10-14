package com.piashcse.route

import com.piashcse.controller.PaymentController
import com.piashcse.models.AddPayment
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.application.*
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
                val requiredParams = listOf("id")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (id) = requiredParams.map { call.parameters[it]!! }
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