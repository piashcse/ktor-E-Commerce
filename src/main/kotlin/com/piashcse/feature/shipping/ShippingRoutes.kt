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
             * @tag Shipping
             * @body [ShippingRequest]
             * @response 200 [Response]
             */
            post {
                val requestBody = call.receive<ShippingRequest>()
                call.respond(
                    ApiResponse.success(
                        shippingController.createShipping(call.currentUser().userId, requestBody),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Shipping
             * @query orderId (required)
             * @response 200 [Response]
             * @response 400
             */
            get {
                val (orderId) = call.requiredParameters("orderId") ?: return@get
                call.respond(
                    ApiResponse.success(
                        shippingController.getShipping(call.currentUser().userId, orderId),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Shipping
             * @path id (required)
             * @query orderId
             * @query address
             * @query city
             * @query country
             * @query shippingMethod
             * @query phone
             * @query email
             * @response 200 [Response]
             * @response 400
             */
            put("{id}") {
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
             * @tag Shipping
             * @path id (required)
             * @response 200 [Response]
             * @response 400
             */
            delete("{id}") {
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