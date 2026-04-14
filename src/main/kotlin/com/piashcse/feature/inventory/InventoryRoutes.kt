package com.piashcse.feature.inventory

import com.piashcse.constants.Message
import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.request.UpdateStockRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.MissingParameterException
import com.piashcse.utils.NotFoundException
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requireParameters
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
                requestBody.validation()
                call.respond(HttpStatusCode.OK, inventoryController.createOrUpdateInventory(requestBody))
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
                val productId = call.requireParameters("id")
                val requestBody = call.receive<InventoryRequest>()
                requestBody.validation()
                call.respond(HttpStatusCode.OK,
                    inventoryController.createOrUpdateInventory(requestBody.copy(productId = productId.first())),
                )
            }

            /**
             * @tag Inventory
             * @description Update stock quantity for a product with add, subtract, or set operation
             * @operationId updateStock
             * @path id (required) Unique identifier of the product
             * @body UpdateStockRequest Stock update request with quantity and operation
             * @response 200 Stock quantity updated successfully
             * @security jwtToken
             */
            put("/stock/{id}") {
                val productId = call.requireParameters("id")
                val requestBody = call.receive<UpdateStockRequest>()
                requestBody.validation()
                call.respond(HttpStatusCode.OK,
                    inventoryController.updateStock(productId.first(), requestBody.quantity, requestBody.operation),
                )
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
                val productId = call.requireParameters("id")
                val inventory = inventoryController.getInventoryByProduct(productId.first())
                    ?: throw NotFoundException(Message.Inventory.NOT_FOUND)
                call.respond(HttpStatusCode.OK, inventory)
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
                val shopId = call.parameters["shopId"]
                    ?: throw MissingParameterException("shopId")
                call.respond(HttpStatusCode.OK, inventoryController.getInventoryByShop(shopId))
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
                call.respond(HttpStatusCode.OK, inventoryController.getLowStockProducts(limit))
            }
        }
    }
}
