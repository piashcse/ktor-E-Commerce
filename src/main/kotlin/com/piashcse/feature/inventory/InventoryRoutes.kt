package com.piashcse.feature.inventory

import com.piashcse.model.request.InventoryRequest
import com.piashcse.plugin.adminAuth
import com.piashcse.plugin.sellerAuth
import com.piashcse.utils.extension.paginationParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Seller inventory management routes.
 */
fun Route.inventoryRoutes(inventoryService: InventoryService) {
    sellerAuth {
        /**
         * @tag Inventory
         * @description Seller: Initialize or update inventory for a product
         */
        post {
            val requestBody = call.receive<InventoryRequest>()
            call.respond(
                HttpStatusCode.OK,
                inventoryService.createOrUpdateInventory(requestBody)
            )
        }

        /**
         * @tag Inventory
         * @description Seller: Update stock quantity
         */
        put("/stock/{productId}") {
            val productId = call.parameters["productId"] ?: return@put call.respond(HttpStatusCode.BadRequest, "productId is required")
            val quantity = call.parameters["quantity"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, "quantity is required and must be an integer")
            val operation = call.parameters["operation"] ?: "set"
            call.respond(
                HttpStatusCode.OK,
                inventoryService.updateStock(productId, quantity, operation)
            )
        }

        /**
         * @tag Inventory
         * @description Seller: Retrieve inventory item details by product ID
         */
        get("/product/{productId}") {
            val productId = call.parameters["productId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "productId is required")
            val inventory = inventoryService.getInventoryByProduct(productId)
            if (inventory != null) {
                call.respond(HttpStatusCode.OK, inventory)
            } else {
                call.respond(HttpStatusCode.NotFound, "Inventory not found for product")
            }
        }

        /**
         * @tag Inventory
         * @description Seller: Retrieve all inventory items for a shop
         */
        get("/shop/{shopId}") {
            val shopId = call.parameters["shopId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "shopId is required")
            val (limit, offset) = call.paginationParameters()
            call.respond(
                HttpStatusCode.OK,
                inventoryService.getInventoryByShop(shopId, limit, offset)
            )
        }

        /**
         * @tag Inventory
         * @description Seller: Retrieve items with stock below a threshold
         */
        get("/low-stock") {
            val (limit, offset) = call.paginationParameters()
            call.respond(
                HttpStatusCode.OK,
                inventoryService.getLowStockProducts(limit, offset)
            )
        }
    }
}

/**
 * Admin inventory routes.
 */
fun Route.inventoryAdminRoutes(inventoryService: InventoryService) {
    adminAuth {
        // Admin specific inventory management could be added here
    }
}
