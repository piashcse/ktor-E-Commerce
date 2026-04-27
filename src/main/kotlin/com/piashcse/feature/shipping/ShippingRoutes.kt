package com.piashcse.feature.shipping

import com.piashcse.model.request.ShippingRequest
import com.piashcse.model.request.UpdateShippingRequest
import com.piashcse.plugin.adminAuth
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * User shipping address routes.
 */
fun Route.shippingRoutes(shippingController: ShippingService) {
    requireRole {
        /**
         * @tag Shipping
         * @description Add a new shipping address for the authenticated user
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
         * @description Retrieve shipping addresses for an order
         */
        get("{orderId}") {
            val orderId = call.parameters["orderId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "orderId is required")
            call.respond(
                HttpStatusCode.OK,
                shippingController.getShipping(call.currentUserId, orderId)
            )
        }

        /**
         * @tag Shipping
         * @description Update an existing shipping address
         */
        put {
            val requestBody = call.receive<UpdateShippingRequest>()
            call.respond(
                HttpStatusCode.OK,
                shippingController.updateShipping(call.currentUserId, requestBody)
            )
        }

        /**
         * @tag Shipping
         * @description Delete a shipping address
         */
        delete("{id}") {
            val (id) = call.requireParameters("id")
            call.respond(
                HttpStatusCode.OK,
                shippingController.deleteShipping(call.currentUserId, id)
            )
        }
    }
}
