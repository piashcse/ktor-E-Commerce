package com.piashcse.feature.shipping_method

import com.piashcse.model.request.ShippingMethodRequest

import com.piashcse.utils.extension.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.shippingMethodAdminRoutes() {
    val shippingMethodRepo: ShippingMethodRepository by inject()
    /**
     * @tag Shipping-Method
     * @description Admin: Create a new shipping method
     */
    post {
        call.respondCreated(shippingMethodRepo.createShippingMethod(call.receive<ShippingMethodRequest>()))
    }

    /**
     * @tag Shipping-Method
     * @description Admin: Update an existing shipping method
     */
    put("/{id}") {
        val id = call.requirePathParameter("id")
        call.respondOk(shippingMethodRepo.updateShippingMethod(id, call.receive<ShippingMethodRequest>()))
    }

    /**
     * @tag Shipping-Method
     * @description Admin: Delete a shipping method
     */
    delete("/{id}") {
        val id = call.requirePathParameter("id")
        call.respondOk(shippingMethodRepo.deleteShippingMethod(id))
    }
}
