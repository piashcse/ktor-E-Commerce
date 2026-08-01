package com.piashcse.feature.inventory

import com.piashcse.constants.Message
import com.piashcse.model.request.InventoryRequest
import com.piashcse.plugin.RateLimitNames
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.requireUserType
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Seller inventory management routes.
 */
fun Route.inventorySellerRoutes() {
    val inventoryService: InventoryService by inject()
    rateLimit(RateLimitName(RateLimitNames.SELLER_WRITE)) {
        /**
         * @tag Inventory
         * @description Seller: Initialize or update inventory for a product
         */
        post {
            call.respondCreated(
                inventoryService.createOrUpdateInventory(
                    userId = call.currentUserId,
                    inventoryRequest = call.receive<InventoryRequest>(),
                ),
            )
        }

        /**
         * @tag Inventory
         * @description Seller: Update stock quantity
         */
        put("/stock/{productId}") {
            val productId = call.requirePathParameter("productId")
            val quantityStr = call.requireQueryParameter("quantity")
            val quantity = quantityStr.toIntOrNull() ?: throw ValidationException(Message.Errors.invalidParameter("quantity", quantityStr))
            val operation = call.parameters["operation"] ?: "set"
            call.respondOk(
                inventoryService.updateStock(
                    userId = call.currentUserId,
                    productId = productId,
                    quantity = quantity,
                    operation = operation,
                ),
            )
        }
    }

    /**
     * @tag Inventory
     * @description Seller: Retrieve inventory item details by product ID
     */
    get("/product/{productId}") {
        val productId = call.requirePathParameter("productId")
        call.respondOk(
            inventoryService.getInventoryByProduct(
                userId = call.currentUserId,
                userType = call.requireUserType(),
                productId = productId,
            ) ?: throw NotFoundException(Message.Inventory.NOT_FOUND),
        )
    }

    /**
     * @tag Inventory
     * @description Seller: Retrieve all inventory items for a shop
     */
    get("/shop/{shopId}") {
        val shopId = call.requirePathParameter("shopId")
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(
            inventoryService.getInventoryByShop(
                userId = call.currentUserId,
                userType = call.requireUserType(),
                shopId = shopId,
                limit = limit,
                offset = offset,
            ),
        )
    }

    /**
     * @tag Inventory
     * @description Seller: Retrieve items with stock below a threshold
     */
    get("/low-stock") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(
            inventoryService.getLowStockProducts(
                userId = call.currentUserId,
                userType = call.requireUserType(),
                limit = limit,
                offset = offset,
            ),
        )
    }
}


