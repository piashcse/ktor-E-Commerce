package com.piashcse.feature.shipping

import com.piashcse.model.request.ShippingRequest
import com.piashcse.model.request.UpdateShippingRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
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
fun Route.shippingRoutes(shippingController: ShippingService) {
    route("/shipping") {

        // Routes for customers to add, retrieve, update, and delete shipping information
        authenticate(RoleManagement.CUSTOMER.role) {

            /**
             * POST request to add shipping information for an order.
             *
             * @tag Shipping
             * @summary auth[customer]
             * @body [ShippingRequest] The shipping details for the order.
             * @response 200 [ApiResponse] Success response
             */
            post("/shipping") {
                val requestBody = call.receive<ShippingRequest>()
                call.respond(
                    ApiResponse.success(
                        shippingController.createShipping(call.currentUser().userId, requestBody),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * GET request to retrieve shipping information for a specific order.
             *
             * @tag Shipping
             * @summary auth[customer]
             * @query orderId The ID of the order to retrieve shipping details.
             * @response 200 [ApiResponse] Success response with shipping details
             * @response 400 Bad request if orderId is missing
             */
            get("/shipping") {
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
             * @tag Shipping
             * @summary auth[customer]
             * @path id The ID of the shipping record to update.
             * @query orderId The updated order ID.
             * @query address The updated shipping address.
             * @query city The updated shipping city.
             * @query country The updated shipping country.
             * @query shippingMethod The updated shipping method.
             * @query phone The updated shipping phone number.
             * @query email The updated shipping recipient email.
             * @response 200 [ApiResponse] Success response with updated shipping info
             * @response 400 Bad request if id is missing
             */
            put("/shipping/{id}") {
                val (id) = call.requiredParameters("id") ?: return@put
                val params = UpdateShippingRequest(
                    id = id,
                    address = call.parameters["address"],
                    city = call.parameters["city"],
                    country = call.parameters["country"],
                    phone = call.parameters["phone"]?.toInt(),
                    email = call.parameters["email"],
                    shippingMethod = call.parameters["shippingMethod"],
                    status = null,
                    trackingNumber = null,
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
             * @tag Shipping
             * @summary auth[customer]
             * @path id The ID of the shipping record to delete.
             * @response 200 [ApiResponse] Success response indicating deletion
             * @response 400 Bad request if id is missing
             */
            delete("/shipping/{id}") {
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