package com.piashcse.feature.shipping

import  com.piashcse.model.request.ShippingRequest
import com.piashcse.model.request.UpdateShippingRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.requireParameters
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

        authenticate(RoleManagement.CUSTOMER.role) {

            /**
             * @tag Shipping
             * @description Create shipping information for an order
             * @operationId createShipping
             * @body ShippingRequest Shipping information request with address and delivery details
             * @response 200 Shipping information created successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<ShippingRequest>()
                call.respond(
                    HttpStatusCode.OK,
                    shippingController.createShipping(call.currentUserId, requestBody)
                )
            }

            /**
             * @tag Shipping
             * @description Retrieve shipping information for a specific order
             * @operationId getShipping
             * @query orderId (required) Unique identifier of the order
             * @response 200 Shipping information retrieved successfully
             * @response 400 Invalid order ID
             * @security jwtToken
             */
            get {
                val orderId = call.requireParameters("orderId")
                call.respond(
                    HttpStatusCode.OK,
                    shippingController.getShipping(call.currentUserId, orderId.first())
                )
            }

            /**
             * @tag Shipping
             * @description Update shipping information for an order
             * @operationId updateShipping
             * @path id (required) Unique identifier of the shipping record to update
             * @query orderId Order ID associated with the shipping
             * @query address Delivery address
             * @query city Delivery city
             * @query country Delivery country
             * @query shippingMethod Shipping method type
             * @query phone Contact phone number
             * @query email Contact email address
             * @response 200 Shipping information updated successfully
             * @response 400 Invalid shipping ID or parameters
             * @security jwtToken
             */
            put("{id}") {
                val id = call.requireParameters("id")
                val params = UpdateShippingRequest(
                    id = id.first(),
                    address = call.parameters["address"],
                    city = call.parameters["city"],
                    country = call.parameters["country"],
                    phone = call.parameters["phone"],
                    email = call.parameters["email"],
                    shippingMethod = call.parameters["shippingMethod"],
                    status = null,
                    trackingNumber = null,
                )

                call.respond(
                    HttpStatusCode.OK,
                    shippingController.updateShipping(call.currentUserId, params)
                )
            }

            /**
             * @tag Shipping
             * @description Delete shipping information for an order
             * @operationId deleteShipping
             * @path id (required) Unique identifier of the shipping record to delete
             * @response 200 Shipping information deleted successfully
             * @response 400 Invalid shipping ID
             * @security jwtToken
             */
            delete("{id}") {
                val id = call.requireParameters("id")
                call.respond(
                    HttpStatusCode.OK,
                    shippingController.deleteShipping(call.currentUserId, id.first())
                )
            }
        }
    }
}
