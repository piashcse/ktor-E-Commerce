package com.piashcse.route

import com.piashcse.controller.ShippingController
import com.piashcse.models.shipping.ShippingRequest
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

/**
 * Defines routes for managing shipping information for a customer's orders.
 *
 * Allows customers to add, retrieve, update, and delete shipping information for their orders.
 *
 * @param shippingController The controller responsible for handling shipping operations.
 */
fun Route.shippingRoute(shippingController: ShippingController) {
    route("/shipping") {

        // Routes for customers to add, retrieve, update, and delete shipping information
        authenticate(RoleManagement.CUSTOMER.role) {

            /**
             * POST request to add shipping information for an order.
             *
             * Accessible by customers only.
             *
             * @param requestBody The shipping details for the order.
             */
            post({
                tags("Shipping")
                request {
                    body<ShippingRequest>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<ShippingRequest>()
                call.respond(
                    ApiResponse.success(
                        shippingController.addShipping(call.currentUser().userId, requestBody),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * GET request to retrieve shipping information for a specific order.
             *
             * Accessible by customers only.
             *
             * @param orderId The ID of the order to retrieve shipping details.
             */
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
                        shippingController.getShipping(call.currentUser().userId, orderId),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * PUT request to update existing shipping information for an order.
             *
             * Accessible by customers only.
             *
             * @param id The ID of the shipping record to update.
             * @param orderId The updated order ID.
             * @param shipAddress The updated shipping address.
             * @param shipCity The updated shipping city.
             * @param shipPhone The updated shipping phone number.
             * @param shipName The updated shipping recipient name.
             * @param shipEmail The updated shipping recipient email.
             * @param shipCountry The updated shipping country.
             */
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
                        shippingController.updateShipping(call.currentUser().userId, params),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * DELETE request to delete a shipping record.
             *
             * Accessible by customers only.
             *
             * @param id The ID of the shipping record to delete.
             */
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