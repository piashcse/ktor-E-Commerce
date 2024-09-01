package com.piashcse.route

import com.piashcse.controller.ShippingController
import com.piashcse.models.shipping.AddShipping
import com.piashcse.models.user.body.JwtTokenBody
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
                val loginUser = call.principal<JwtTokenBody>()
                val requiredParams = listOf("orderId", "shipAddress")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (orderId, shipAddress) = requiredParams.map { call.parameters[it]!! }
                /*call.respond(
                    ApiResponse.success(
                        shippingController.updateShipping(loginUser?.userId!!, orderId), HttpStatusCode.OK
                    )
                )*/
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
}/*
fun NormalOpenAPIRoute.shippingRoute(shippingController: ShippingController) {
    route("shipping") {
        authenticateWithJwt(RoleManagement.CUSTOMER.role) {
            post<Unit, Response, AddShipping, JwtTokenBody> { _, requestBody ->
                requestBody.validation()
                respond(
                    ApiResponse.success(
                        shippingController.addShipping(principal().userId, requestBody), HttpStatusCode.OK
                    )
                )
            }
            get<OrderId, Response, JwtTokenBody> { params ->
                respond(
                    ApiResponse.success(
                        shippingController.getShipping(principal().userId, params.orderId), HttpStatusCode.OK
                    )
                )
            }
            route("/{orderId}").put<UpdateShipping, Response, Unit, JwtTokenBody> { params, _ ->
                params.validation()
                shippingController.updateShipping(
                    principal().userId, params
                ).let {
                    respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }
            delete<OrderId, Response, JwtTokenBody> { params ->
                params.validation()
                respond(
                    ApiResponse.success(
                        shippingController.deleteShipping(principal().userId, params.orderId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}*/
