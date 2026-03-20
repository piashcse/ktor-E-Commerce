package com.piashcse.feature.inventory

import com.piashcse.model.request.InventoryRequest
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
 * Defines routes for managing inventory.
 *
 * @param inventoryController The controller handling inventory-related operations.
 */
fun Route.inventoryRoutes(inventoryController: InventoryService) {
    route("/inventory") {
        authenticate(RoleManagement.SELLER.role) {

            /**
             * @tag Inventory
             * @description Create new inventory record or update existing one for a product
             * @operationId createOrUpdateInventory
             * @body InventoryRequest Inventory request with product ID and stock details
             * @response 200 Inventory created or updated successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<InventoryRequest>()
                call.respond(ApiResponse.success(inventoryController.createOrUpdateInventory(requestBody), HttpStatusCode.OK))
            }

            /**
             * @tag Inventory
             * @description Update inventory for a specific product by ID
             * @operationId updateInventory
             * @path id (required) Unique identifier of the product
             * @body InventoryRequest Inventory request with updated stock details
             * @response 200 Inventory updated successfully
             * @security jwtToken
             */
            put("/{id}") {
                val (productId) = call.requiredParameters("id") ?: return@put
                val requestBody = call.receive<InventoryRequest>()
                // For this route, we'll update the inventory by product ID
                call.respond(ApiResponse.success(
                    inventoryController.createOrUpdateInventory(requestBody.copy(productId = productId)), 
                    HttpStatusCode.OK
                ))
            }

            /**
             * @tag Inventory
             * @description Update stock quantity for a product with add, subtract, or set operation
             * @operationId updateStock
             * @path id (required) Unique identifier of the product
             * @query quantity (required) Quantity value to add, subtract, or set
             * @query operation (optional) Stock operation type: add, subtract, or set (default: add)
             * @response 200 Stock quantity updated successfully
             * @security jwtToken
             */
            put("/stock/{id}") {
                val (productId) = call.requiredParameters("id") ?: return@put
                val quantity = call.parameters["quantity"]?.toIntOrNull() ?: return@put
                val operation = call.parameters["operation"] ?: "add"
                call.respond(ApiResponse.success(
                    inventoryController.updateStock(productId, quantity, operation), 
                    HttpStatusCode.OK
                ))
            }

            /**
             * @tag Inventory
             * @description Retrieve inventory details for a specific product
             * @operationId getInventoryByProduct
             * @path id (required) Unique identifier of the product
             * @response 200 Inventory details retrieved successfully
             * @response 404 Product inventory not found
             * @security jwtToken
             */
            get("/{id}") {
                val (productId) = call.requiredParameters("id") ?: return@get
                val inventory = inventoryController.getInventoryByProduct(productId)
                if (inventory != null) {
                    call.respond(ApiResponse.success(inventory, HttpStatusCode.OK))
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        authenticate(RoleManagement.ADMIN.role) {

            /**
             * @tag Inventory
             * @description Admin-only: Retrieve all inventory items for a specific shop
             * @operationId getInventoryByShop
             * @query shopId (required) Unique identifier of the shop
             * @response 200 Shop inventory retrieved successfully
             * @security jwtToken
             */
            get("/shop") {
                val shopId = call.parameters["shopId"] ?: return@get
                call.respond(ApiResponse.success(inventoryController.getInventoryByShop(shopId), HttpStatusCode.OK))
            }

            /**
             * @tag Inventory
             * @description Admin-only: Retrieve products with low stock levels
             * @operationId getLowStockProducts
             * @query limit Maximum number of low stock products to return (default 10)
             * @response 200 Low stock products retrieved successfully
             * @security jwtToken
             */
            get("/low-stock") {
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 10
                call.respond(ApiResponse.success(inventoryController.getLowStockProducts(limit), HttpStatusCode.OK))
            }
        }
    }
}