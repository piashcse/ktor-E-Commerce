package com.piashcse.feature.inventory

import com.piashcse.constants.InventoryStatus
import com.piashcse.constants.Message
import com.piashcse.database.entities.InventoryDAO
import com.piashcse.database.entities.InventoryTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.response.InventoryResponse
import com.piashcse.utils.NotFoundException
import com.piashcse.utils.ValidationException
import com.piashcse.utils.throwNotFound
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.update

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
        val product = ProductDAO.findById(inventoryRequest.productId) ?: inventoryRequest.productId.throwNotFound("Product")
        val shop = ShopDAO.findById(inventoryRequest.shopId) ?: inventoryRequest.shopId.throwNotFound("Shop")

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
        if (request.productId.isBlank()) throw ValidationException(Message.Validation.blankField("Product ID"))
        if (request.shopId.isBlank()) throw ValidationException(Message.Validation.blankField("Shop ID"))
        if (request.stockQuantity < 0) throw ValidationException(Message.Inventory.NEGATIVE_STOCK)
        if (request.minimumStockLevel != null && request.minimumStockLevel!! < 0) throw ValidationException(Message.Validation.negativeValue("Minimum stock level"))
        if (request.maximumStockLevel != null && request.maximumStockLevel!! < 0) throw ValidationException(Message.Validation.negativeValue("Maximum stock level"))
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
            ?: productId.throwNotFound("Product")

        require(quantity > 0) { "Quantity must be positive for $operation operation" }
        requireValidOperation(operation)

        val newStock = computeNewStock(inventory.stockQuantity, quantity, operation)
        updateStockQuantity(inventory.id.value, newStock, inventory.stockQuantity, quantity)
        refreshInventoryStatus(productId)
    }

    private fun computeNewStock(current: Int, quantity: Int, operation: String): Int = when (operation.lowercase()) {
        "add" -> current + quantity
        "subtract" -> current.takeIf { it >= quantity }
            ?: throw NotFoundException(Message.Inventory.insufficientStock(current, quantity))
        "set" -> quantity.also { require(it >= 0) { "Quantity cannot be negative for set operation" } }
        else -> throw NotFoundException(Message.Inventory.invalidOperation(operation))
    }

    private fun updateStockQuantity(inventoryId: String, newStock: Int, currentStock: Int, requestedQty: Int) {
        InventoryTable.update({ InventoryTable.id eq EntityID(inventoryId, InventoryTable) }) {
            it[stockQuantity] = newStock
        }.takeIf { it > 0 }
            ?: throw NotFoundException(Message.Inventory.insufficientStock(currentStock, requestedQty))
    }

    private fun refreshInventoryStatus(productId: String): InventoryResponse {
        val updated = InventoryDAO.find { InventoryTable.productId eq productId }.singleOrNull()
            ?: throw NotFoundException(Message.Inventory.NOT_FOUND)
        updated.status = determineInventoryStatus(updated.stockQuantity, updated.minimumStockLevel)
        return updated.response()
    }

    private fun requireValidOperation(operation: String) {
        if (operation.lowercase() !in listOf("add", "subtract", "set")) {
            throw NotFoundException(Message.Inventory.invalidOperation(operation))
        }
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
        }.orderBy(InventoryTable.stockQuantity to SortOrder.ASC)
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