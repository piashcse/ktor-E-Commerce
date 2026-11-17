@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.shipping_method

import com.piashcse.model.request.ShippingMethodRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.requireSellerShopId
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondDeleted
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

fun Route.shippingMethodAdminRoutes() {
    val shippingMethodRepo: ShippingMethodRepository by inject()
    adminWriteRateLimit {
        /**
         * Admin - Create a new shipping method
         *
         */
        post {
            call.respondCreated(shippingMethodRepo.createShippingMethod(call.receive<ShippingMethodRequest>()))
        }.describe { summary = "Admin - Create a new shipping method" }

        /**
         * Admin - Update an existing shipping method
         *
         */
        put("/{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(shippingMethodRepo.updateShippingMethod(id, call.receive<ShippingMethodRequest>()))
        }.describe { summary = "Admin - Update an existing shipping method" }

        /**
         * Admin - Delete a shipping method
         *
         */
        delete("/{id}") {
            val id = call.requirePathParameter("id")
            shippingMethodRepo.deleteShippingMethod(id)
            call.respondDeleted("Shipping method deleted successfully")
        }.describe { summary = "Admin - Delete a shipping method" }
    }

    /**
     * Admin - Retrieve all shipping methods
     *
     */
    get {
        val (limit, offset) = call.pageRequest()
        call.respondOk(shippingMethodRepo.getShippingMethods(limit, offset))
    }.describe { summary = "Admin - Retrieve all shipping methods" }
}

/** Seller shipping rates scoped to the seller's shop. */
fun Route.sellerShippingMethodRoutes() {
    val shippingMethodRepo: ShippingMethodRepository by inject()
    get {
        val (limit, offset) = call.pageRequest()
        val shopId = requireSellerShopId(call.currentUserId)
        call.respondOk(shippingMethodRepo.getSellerShippingMethods(shopId, limit, offset))
    }.describe { summary = "Seller - List my shipping methods" }

    sellerWriteRateLimit {
        post {
            val shopId = requireSellerShopId(call.currentUserId)
            call.respondCreated(shippingMethodRepo.createSellerShippingMethod(shopId, call.receive<ShippingMethodRequest>()))
        }.describe {
            summary = "Seller - Create a shipping method"
            requestBody { jsonBody<ShippingMethodRequest>() }
        }
        put("/{id}") {
            val shopId = requireSellerShopId(call.currentUserId)
            call.respondOk(
                shippingMethodRepo.updateSellerShippingMethod(
                    shopId,
                    call.requirePathParameter("id"),
                    call.receive<ShippingMethodRequest>(),
                ),
            )
        }.describe {
            summary = "Seller - Update a shipping method"
            requestBody { jsonBody<ShippingMethodRequest>() }
        }
        delete("/{id}") {
            val shopId = requireSellerShopId(call.currentUserId)
            shippingMethodRepo.deleteSellerShippingMethod(shopId, call.requirePathParameter("id"))
            call.respondDeleted("Shipping method deleted")
        }.describe { summary = "Seller - Delete a shipping method" }
    }
}
