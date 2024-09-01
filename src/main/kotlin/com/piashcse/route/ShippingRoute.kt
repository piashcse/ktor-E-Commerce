package com.piashcse.route

import com.piashcse.controller.ShippingController
import com.piashcse.models.shipping.AddShipping
import com.piashcse.models.shipping.UpdateShipping
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.getCurrentUser
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.shippingRoute(shippingController: ShippingController) {
    route("/shipping") {
        authenticate(RoleManagement.CUSTOMER.role) {
            post("", {
                tags("Shipping")
                request {
                    body<AddShipping>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddShipping>()
                call.respond(
                    ApiResponse.success(
                        shippingController.addShipping(getCurrentUser().userId, requestBody), HttpStatusCode.OK
                    )
                )
            }
            get("", {
                tags("Shipping")
                request {
                    queryParameter<String>("orderId") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("orderId")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (orderId) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        shippingController.getShipping(getCurrentUser().userId, orderId), HttpStatusCode.OK
                    )
                )
            }
            put("", {
                tags("Shipping")
                request {
                    queryParameter<String>("orderId") {
                        required = true
                    }
                    queryParameter<String>("shipAddress")
                    queryParameter<String>("shipCity")
                    queryParameter<String>("shipPhone")
                    queryParameter<String>("shipName")
                    queryParameter<String>("shipEmail")
                    queryParameter<String>("shipCountry")
                }
                apiResponse()
            }) {
                val params = UpdateShipping(
                    orderId = call.parameters["orderId"] ?: "",
                    shipAddress = call.parameters["shipAddress"] ?: "",
                    shipCity = call.parameters["shipCity"],
                    shipPhone = call.parameters["shipPhone"]?.toInt(),
                    shipName = call.parameters["shipName"],
                    shipEmail = call.parameters["shipEmail"],
                    shipCountry = call.parameters["shipCountry"],
                )

                call.respond(
                    ApiResponse.success(
                        shippingController.updateShipping(getCurrentUser().userId, params), HttpStatusCode.OK
                    )
                )
            }
            delete("", {
                tags("Shipping")
                request {
                    queryParameter<String>("orderId") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("orderId")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (orderId) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        shippingController.deleteShipping(getCurrentUser().userId, orderId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}