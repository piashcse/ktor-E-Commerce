package com.piashcse.route

import com.piashcse.controller.ShippingController
import com.piashcse.models.shipping.AddShipping
import com.piashcse.models.shipping.UpdateShipping
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.shippingRoute(shippingController: ShippingController) {
    route("/shipping") {
        authenticate(RoleManagement.CUSTOMER.role) {
            post({
                tags("Shipping")
                request {
                    body<AddShipping>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddShipping>()
                call.respond(
                    ApiResponse.success(
                        shippingController.addShipping(call.currentUser().userId, requestBody), HttpStatusCode.OK
                    )
                )
            }
            get({
                tags("Shipping")
                request {
                    queryParameter<String>("orderId") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("orderId")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (orderId) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        shippingController.getShipping(call.currentUser().userId, orderId), HttpStatusCode.OK
                    )
                )
            }
            put("{id}", {
                tags("Shipping")
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                    queryParameter<String>("orderId")
                    queryParameter<String>("shipAddress")
                    queryParameter<String>("shipCity")
                    queryParameter<String>("shipPhone")
                    queryParameter<String>("shipName")
                    queryParameter<String>("shipEmail")
                    queryParameter<String>("shipCountry")
                }
                apiResponse()
            }) {
                val requiredParams = listOf("id")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (id) = requiredParams.map { call.parameters[it]!! }
                val params = UpdateShipping(
                    id = id,
                    shipAddress = call.parameters["shipAddress"],
                    shipCity = call.parameters["shipCity"],
                    shipPhone = call.parameters["shipPhone"]?.toInt(),
                    shipName = call.parameters["shipName"],
                    shipEmail = call.parameters["shipEmail"],
                    shipCountry = call.parameters["shipCountry"],
                )

                call.respond(
                    ApiResponse.success(
                        shippingController.updateShipping(call.currentUser().userId, params), HttpStatusCode.OK
                    )
                )
            }
            delete("{id}", {
                tags("Shipping")
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
                        shippingController.deleteShipping(call.currentUser().userId, id), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}