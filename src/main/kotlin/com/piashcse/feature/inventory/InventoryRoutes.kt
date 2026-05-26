package com.piashcse.feature.inventory

import com.piashcse.model.request.InventoryRequest
import com.piashcse.utils.extension.paginateQueryParams
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Seller inventory management routes.
 */
fun Route.inventorySellerRoutes(inventoryService: InventoryService) {
    /**
     * @tag Inventory
     * @description Seller: Initialize or update inventory for a product
     */
    post {
        val requestBody = call.receive<InventoryRequest>()
        call.respond(
            HttpStatusCode.OK,
            inventoryService.createOrUpdateInventory(requestBody),
        )
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
        call.respond(
            HttpStatusCode.OK,
            inventoryService.updateStock(productId, quantity, operation),
        )
    }

    /**
     * @tag Inventory
     * @description Seller: Retrieve inventory item details by product ID
     */
    get("/product/{productId}") {
        val productId = call.requirePathParameter("productId")
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
        val shopId = call.requirePathParameter("shopId")
        val (limit, offset) = call.paginateQueryParams()
        call.respond(
            HttpStatusCode.OK,
            inventoryService.getInventoryByShop(shopId, limit, offset),
        )
    }

    /**
     * @tag Inventory
     * @description Seller: Retrieve items with stock below a threshold
     */
    get("/low-stock") {
        val (limit, offset) = call.paginateQueryParams()
        call.respond(
            HttpStatusCode.OK,
            inventoryService.getLowStockProducts(limit, offset),
        )
    }
}

/**
 * Admin inventory routes.
 */
fun Route.inventoryAdminRoutes(inventoryService: InventoryService) {
    // Admin specific inventory management could be added here
}
