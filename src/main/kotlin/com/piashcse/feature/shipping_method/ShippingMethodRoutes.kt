package com.piashcse.feature.shipping_method

import com.piashcse.model.request.ShippingMethodRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.shippingMethodAdminRoutes(shippingMethodService: ShippingMethodService) {
    /**
     * @tag Shipping-Method
     * @description Admin: Create a new shipping method
     */
    post {
        val requestBody = call.receive<ShippingMethodRequest>()
        requestBody.validation()
        call.respond(
            HttpStatusCode.Created,
            shippingMethodService.createShippingMethod(requestBody),
        )
    }

    /**
     * @tag Shipping-Method
     * @description Admin: Update an existing shipping method
     */
    put("/{id}") {
        val id = call.requirePathParameter("id")
        val requestBody = call.receive<ShippingMethodRequest>()
        requestBody.validation()
        call.respond(
            HttpStatusCode.OK,
            shippingMethodService.updateShippingMethod(id, requestBody),
        )
    }

    /**
     * @tag Shipping-Method
     * @description Admin: Delete a shipping method
     */
    delete("/{id}") {
        val id = call.requirePathParameter("id")
        call.respond(
            HttpStatusCode.OK,
            shippingMethodService.deleteShippingMethod(id),
        )
    }
}
