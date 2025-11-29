package com.piashcse.feature.inventory

import com.piashcse.constants.InventoryStatus
import com.piashcse.database.entities.InventoryDAO
import com.piashcse.database.entities.InventoryTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.response.InventoryResponse
import com.piashcse.utils.ValidationException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq

class InventoryService : InventoryRepository {
    /**
     * Creates or updates inventory for a product.
     *
     * @param inventoryRequest The inventory details to create or update.
     * @return The created/updated inventory record.
     */
    override suspend fun createOrUpdateInventory(inventoryRequest: InventoryRequest): InventoryResponse = query {
        // Validate inputs
        validateInventoryRequest(inventoryRequest)

        // Validate related entities exist
        val product = ProductDAO.findById(inventoryRequest.productId) ?: throw inventoryRequest.productId.notFoundException()
        val shop = ShopDAO.findById(inventoryRequest.shopId) ?: throw inventoryRequest.shopId.notFoundException()

        val existingInventory = InventoryDAO.find {
            InventoryTable.productId eq inventoryRequest.productId
        }.singleOrNull()

        val inventory = if (existingInventory != null) {
            // Update existing inventory
            existingInventory.apply {
                stockQuantity = inventoryRequest.stockQuantity
                minimumStockLevel = inventoryRequest.minimumStockLevel ?: minimumStockLevel
                maximumStockLevel = inventoryRequest.maximumStockLevel ?: maximumStockLevel
                status = determineInventoryStatus(stockQuantity, minimumStockLevel)
            }
        } else {
            // Create new inventory
            InventoryDAO.new {
                productId = org.jetbrains.exposed.v1.core.dao.id.EntityID(inventoryRequest.productId, InventoryTable)
                shopId = org.jetbrains.exposed.v1.core.dao.id.EntityID(inventoryRequest.shopId, InventoryTable)
                stockQuantity = inventoryRequest.stockQuantity
                minimumStockLevel = inventoryRequest.minimumStockLevel ?: 10
                maximumStockLevel = inventoryRequest.maximumStockLevel ?: 1000
                status = determineInventoryStatus(inventoryRequest.stockQuantity, minimumStockLevel ?: 10)
            }
        }

        inventory.response()
    }

    private fun validateInventoryRequest(request: InventoryRequest) {
        if (request.productId.isBlank()) throw ValidationException("Product ID cannot be blank")
        if (request.shopId.isBlank()) throw ValidationException("Shop ID cannot be blank")
        if (request.stockQuantity < 0) throw ValidationException("Stock quantity cannot be negative")
        if (request.minimumStockLevel != null && request.minimumStockLevel!! < 0) throw ValidationException("Minimum stock level cannot be negative")
        if (request.maximumStockLevel != null && request.maximumStockLevel!! < 0) throw ValidationException("Maximum stock level cannot be negative")
    }

    /**
     * Gets inventory by product ID.
     *
     * @param productId The product ID to get inventory for.
     * @return The inventory record for the product.
     */
    override suspend fun getInventoryByProduct(productId: String): InventoryResponse? = query {
        val inventory = InventoryDAO.find { InventoryTable.productId eq productId }.singleOrNull()
        inventory?.response()
    }

    /**
     * Updates inventory stock for a product.
     *
     * @param productId The product ID to update.
     * @param quantity The quantity to update by.
     * @param operation The operation to perform: "add", "subtract", or "set".
     * @return The updated inventory record.
     */
    override suspend fun updateStock(productId: String, quantity: Int, operation: String): InventoryResponse = query {
        val inventory = InventoryDAO.find { InventoryTable.productId eq productId }.singleOrNull()
            ?: throw productId.notFoundException()

        val updatedStock = when (operation.lowercase()) {
            "add" -> inventory.stockQuantity + quantity
            "subtract" -> {
                val newStock = inventory.stockQuantity - quantity
                if (newStock < 0) 0 else newStock
            }
            "set" -> quantity
            else -> inventory.stockQuantity
        }

        inventory.apply {
            stockQuantity = updatedStock
            status = determineInventoryStatus(stockQuantity, minimumStockLevel)
        }


        inventory.response()
    }

    /**
     * Gets low stock products.
     *
     * @param limit The maximum number of products to return.
     * @return A list of products with low stock.
     */
    override suspend fun getLowStockProducts(limit: Int): List<InventoryResponse> = query {
        InventoryDAO.find {
            InventoryTable.stockQuantity lessEq InventoryTable.minimumStockLevel
        }.orderBy(InventoryTable.stockQuantity to org.jetbrains.exposed.v1.core.SortOrder.ASC)
         .limit(limit)
         .map { it.response() }
    }

    /**
     * Gets inventory by shop ID.
     *
     * @param shopId The shop ID to get inventory for.
     * @return A list of inventory records for the shop.
     */
    override suspend fun getInventoryByShop(shopId: String): List<InventoryResponse> = query {
        InventoryDAO.find { InventoryTable.shopId eq shopId }.map { it.response() }
    }

    private fun determineInventoryStatus(stock: Int, minLevel: Int): InventoryStatus {
        return when {
            stock <= 0 -> InventoryStatus.OUT_OF_STOCK
            stock <= minLevel -> InventoryStatus.LOW_STOCK
            else -> InventoryStatus.IN_STOCK
        }
    }
}