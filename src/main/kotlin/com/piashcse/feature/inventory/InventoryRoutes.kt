package com.piashcse.feature.inventory

import com.piashcse.model.request.InventoryRequest
import com.piashcse.plugin.RateLimitNames
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.http.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Seller inventory management routes.
 */
fun Route.inventorySellerRoutes() {
    val inventoryRepo: InventoryRepository by inject()
    rateLimit(RateLimitName(RateLimitNames.SELLER_WRITE)) {
        /**
         * @tag Inventory
         * @description Seller: Initialize or update inventory for a product
         */
        post {
            call.respondCreated(inventoryRepo.createOrUpdateInventory(call.receive<InventoryRequest>()))
        }

        /**
         * @tag Inventory
         * @description Seller: Update stock quantity
         */
        put("/stock/{productId}") {
            val productId = call.requirePathParameter("productId")
            val quantityStr = call.requireQueryParameter("quantity")
            val quantity = quantityStr.toIntOrNull() ?: throw IllegalArgumentException("quantity must be an integer")
            val operation = call.parameters["operation"] ?: "set"
            call.respondOk(inventoryRepo.updateStock(productId, quantity, operation))
        }
    }

    /**
     * @tag Inventory
     * @description Seller: Retrieve inventory item details by product ID
     */
    get("/product/{productId}") {
        val productId = call.requirePathParameter("productId")
        val inventory = inventoryRepo.getInventoryByProduct(productId)
        if (inventory != null) {
            call.respondOk(inventory)
        } else {
            call.respond(HttpStatusCode.NotFound, "Inventory not found for product")
        }
    }

    /**
     * @tag Inventory
     * @description Seller: Retrieve all inventory items for a shop
     */
    get("/shop/{shopId}") {
        val shopId = call.requirePathParameter("shopId")
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(inventoryRepo.getInventoryByShop(shopId, limit, offset))
    }

    /**
     * @tag Inventory
     * @description Seller: Retrieve items with stock below a threshold
     */
    get("/low-stock") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(inventoryRepo.getLowStockProducts(limit, offset))
    }
}


