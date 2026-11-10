@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.inventory

import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.response.InventoryResponse
import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.service.CatalogCache
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.queryInt
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import io.ktor.http.*
import io.ktor.openapi.jsonSchema
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/**
 * Seller inventory management routes.
 */
fun Route.inventorySellerRoutes() {
    val inventoryRepo: InventoryRepository by inject()
    sellerWriteRateLimit {
        /**
         * Seller - Initialize or update inventory for a product
         *
         */
        post {
            val result = inventoryRepo.createOrUpdateInventory(call.receive<InventoryRequest>(), call.currentUserId)
            CatalogCache.invalidate()
            call.respondCreated(result)
        }.describe {
            summary = "Create or update inventory"
            description = "Optionally scoped to a product variant and warehouse"
            requestBody { jsonBody<InventoryRequest>() }
            responses { created<InventoryResponse>("Inventory upserted") }
        }

        /**
         * Seller - Update stock quantity
         *
         */
        put("/stock/{productId}") {
            val productId = call.requirePathParameter("productId")
            val quantity =
                call.queryInt("quantity")
                    ?: throw IllegalArgumentException("quantity must be an integer")
            val operation = call.parameters["operation"] ?: "set"
            val warehouseId = call.request.queryParameters["warehouseId"]
            val variantId = call.request.queryParameters["variantId"]
            val result = inventoryRepo.updateStock(call.currentUserId, productId, quantity, operation, warehouseId, variantId)
            CatalogCache.invalidate()
            call.respondOk(result)
        }.describe {
            summary = "Update stock"
            description = "Atomic add/subtract/set; pass variantId for variant-level stock"
            parameters {
                query("quantity") {
                    required = true
                    description = "Positive quantity"
                    schema = jsonSchema<Int>()
                }
                query("operation") { description = "add | subtract | set (default: set)" }
                query("warehouseId") { description = "Optional warehouse; defaults to the first" }
                query("variantId") { description = "Optional product variant" }
            }
            responses { ok<InventoryResponse>("Updated inventory") }
        }
    }

    /**
     * Seller - Retrieve inventory item details by product ID
     *
     */
    get("/product/{productId}") {
        val productId = call.requirePathParameter("productId")
        val variantId = call.request.queryParameters["variantId"]
        val inventory = inventoryRepo.getInventoryByProduct(productId, call.currentUserId, variantId)
        if (inventory != null) {
            call.respondOk(inventory)
        } else {
            throw NotFoundException("Inventory not found for product")
        }
    }.describe {
        summary = "Get product inventory"
        description = "Pass variantId for variant-level rows"
        parameters { query("variantId") { description = "Optional product variant" } }
        responses { ok<InventoryResponse>("Inventory row") }
    }

    /**
     * Seller - Retrieve all inventory items for a shop
     *
     */
    get("/shop/{shopId}") {
        val shopId = call.requirePathParameter("shopId")
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(inventoryRepo.getInventoryByShop(call.currentUserId, shopId, limit, offset))
    }.describe { summary = "List shop inventory" }

    /**
     * Seller - Retrieve items with stock below a threshold
     *
     */
    get("/low-stock") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(inventoryRepo.getLowStockProducts(call.currentUserId, limit, offset))
    }.describe { summary = "List low-stock inventory" }
}

/**
 * Admin inventory routes.
 */
fun Route.inventoryAdminRoutes() {
    val inventoryRepo: InventoryRepository by inject()
    /**
     * Admin - Retrieve all inventory items across shops, optionally filtered by shop
     *
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(inventoryRepo.getAllInventory(limit, offset, call.request.queryParameters["shopId"]))
    }.describe {
        summary = "Admin: list inventory"
        description = "Optional shop filter"
    }
}
