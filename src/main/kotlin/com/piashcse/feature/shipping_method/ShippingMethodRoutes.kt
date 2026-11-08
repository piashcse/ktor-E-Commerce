package com.piashcse.feature.shipping_method

import com.piashcse.model.request.ShippingMethodRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.shippingMethodAdminRoutes() {
    val shippingMethodRepo: ShippingMethodRepository by inject()
    adminWriteRateLimit {
        /**
         * Admin: Create a new shipping method
         *
         * @tag Shipping Method
         * @description Admin: Create a new shipping method
         */
        post {
            call.respondCreated(shippingMethodRepo.createShippingMethod(call.receive<ShippingMethodRequest>()))
        }

        /**
         * Admin: Update an existing shipping method
         *
         * @tag Shipping Method
         * @description Admin: Update an existing shipping method
         */
        put("/{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(shippingMethodRepo.updateShippingMethod(id, call.receive<ShippingMethodRequest>()))
        }

        /**
         * Admin: Delete a shipping method
         *
         * @tag Shipping Method
         * @description Admin: Delete a shipping method
         */
        delete("/{id}") {
            val id = call.requirePathParameter("id")
            shippingMethodRepo.deleteShippingMethod(id)
            call.respondOk(com.piashcse.utils.common.MessageResponse("Shipping method deleted successfully"))
        }
    }

    /**
     * Admin: Retrieve all shipping methods
     *
     * @tag Shipping Method
     * @description Admin: Retrieve all shipping methods
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(shippingMethodRepo.getShippingMethods(limit, offset))
    }
}
