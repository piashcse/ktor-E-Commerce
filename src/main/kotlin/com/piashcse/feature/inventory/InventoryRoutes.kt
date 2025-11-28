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
    // Main route for inventory management
    route("/inventory") {
        
        // Routes for sellers to manage their inventory
        authenticate(RoleManagement.SELLER.role) {
            
            /**
             * @tag Inventory
             * @body requestBody
             * @response 200 [Response]
             */
            post {
                val requestBody = call.receive<InventoryRequest>()
                call.respond(ApiResponse.success(inventoryController.createOrUpdateInventory(requestBody), HttpStatusCode.OK))
            }

            /**
             * @tag Inventory
             * @path productId (required)
             * @body requestBody
             * @response 200 [Response]
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
             * @path productId (required)
             * @query quantity (required)
             * @query operation (optional) - add, subtract, set
             * @response 200 [Response]
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
             * @path productId (required)
             * @response 200 [Response]
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

        // Routes for admins to manage all inventory
        authenticate(RoleManagement.ADMIN.role) {
            
            /**
             * @tag Inventory
             * @query shopId (required)
             * @response 200 [Response]
             */
            get("/shop") {
                val shopId = call.parameters["shopId"] ?: return@get
                call.respond(ApiResponse.success(inventoryController.getInventoryByShop(shopId), HttpStatusCode.OK))
            }

            /**
             * @tag Inventory
             * @query limit
             * @response 200 [Response]
             */
            get("/low-stock") {
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 10
                call.respond(ApiResponse.success(inventoryController.getLowStockProducts(limit), HttpStatusCode.OK))
            }
        }
    }
}