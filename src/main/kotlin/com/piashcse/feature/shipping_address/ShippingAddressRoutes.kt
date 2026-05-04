package com.piashcse.feature.shipping_address

import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.shippingAddressRoutes(shippingAddressService: ShippingAddressService) {
    requireRole {
        /**
         * @tag ShippingAddress
         * @description Add a new shipping address for the authenticated user
         */
        post {
            val requestBody = call.receive<ShippingAddressRequest>()
            requestBody.validation()
            call.respond(
                HttpStatusCode.Created,
                shippingAddressService.createShippingAddress(call.currentUserId, requestBody)
            )
        }

        /**
         * @tag ShippingAddress
         * @description Retrieve all shipping addresses for the authenticated user
         */
        get {
            call.respond(
                HttpStatusCode.OK,
                shippingAddressService.getShippingAddresses(call.currentUserId)
            )
        }

        /**
         * @tag ShippingAddress
         * @description Update an existing shipping address
         */
        put("/{id}") {
            val (id) = call.requireParameters("id")
            val requestBody = call.receive<ShippingAddressRequest>()
            requestBody.validation()
            call.respond(
                HttpStatusCode.OK,
                shippingAddressService.updateShippingAddress(call.currentUserId, id, requestBody)
            )
        }

        /**
         * @tag ShippingAddress
         * @description Delete a shipping address
         */
        delete("/{id}") {
            val (id) = call.requireParameters("id")
            call.respond(
                HttpStatusCode.OK,
                shippingAddressService.deleteShippingAddress(call.currentUserId, id)
            )
        }
    }
}
