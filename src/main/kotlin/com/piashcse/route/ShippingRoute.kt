package com.piashcse.route

import com.piashcse.controller.ShippingController
import com.piashcse.models.shipping.AddShipping
import com.piashcse.models.shipping.UpdateShipping
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
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
                val (orderId) = call.requiredParameters("orderId") ?: return@get
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
                val (id) = call.requiredParameters("id") ?: return@put
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
                val (id) = call.requiredParameters("id") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        shippingController.deleteShipping(call.currentUser().userId, id), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}