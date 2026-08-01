package com.piashcse.feature.inventory

import com.piashcse.constants.InventoryStatus
import com.piashcse.constants.Message
import com.piashcse.constants.StockOperation
import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.response.InventoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.requireNotBlank
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.validator.ValidationException

class InventoryService(private val inventoryRepo: InventoryRepository) {

    /**
     * Updates inventory stock, validating the operation and quantity and
     * enforcing the insufficient-stock rule. Runs under a row lock in a
     * retryable transaction.
     */
    suspend fun updateStock(
        productId: String,
        quantity: Int,
        operation: String = "add",
    ): InventoryResponse = suspendRetryQuery {
        val inventory = inventoryRepo.getInventoryForUpdate(productId)
        val op = operation.parseEnum<StockOperation>("operation")

        if (quantity <= 0) throw ValidationException(Message.Inventory.quantityNotPositive(operation))

        val newStock = when (op) {
            StockOperation.ADD -> inventory.stockQuantity + quantity
            StockOperation.SUBTRACT -> {
                if (inventory.stockQuantity < quantity)
                    throw ValidationException(Message.Inventory.insufficientStock(inventory.stockQuantity, quantity))
                inventory.stockQuantity - quantity
            }
            StockOperation.SET -> {
                if (quantity < 0) throw ValidationException(Message.Inventory.NEGATIVE_QUANTITY)
                quantity
            }
        }

        val status = InventoryStatus.fromStockLevel(newStock, inventory.minimumStockLevel)
        inventoryRepo.setStock(productId, newStock, status)
    }

    /**
     * Creates or updates inventory for a product, validating the request inputs.
     * Runs in a retryable transaction.
     */
    suspend fun createOrUpdateInventory(inventoryRequest: InventoryRequest): InventoryResponse = suspendRetryQuery {
        inventoryRequest.productId.requireNotBlank("Product ID")
        inventoryRequest.shopId.requireNotBlank("Shop ID")
        if (inventoryRequest.stockQuantity < 0) throw ValidationException(Message.Inventory.NEGATIVE_STOCK)
        if (inventoryRequest.minimumStockLevel != null && inventoryRequest.minimumStockLevel < 0)
            throw ValidationException(Message.Validation.negativeValue("Minimum stock level"))
        if (inventoryRequest.maximumStockLevel != null && inventoryRequest.maximumStockLevel < 0)
            throw ValidationException(Message.Validation.negativeValue("Maximum stock level"))

        inventoryRepo.createOrUpdateInventory(inventoryRequest)
    }

    suspend fun getInventoryByProduct(productId: String): InventoryResponse? = inventoryRepo.getInventoryByProduct(productId)

    suspend fun getLowStockProducts(
        limit: Int = 10,
        offset: Int = 0,
    ): PaginatedResponse<InventoryResponse> = inventoryRepo.getLowStockProducts(limit, offset)

    suspend fun getInventoryByShop(
        shopId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<InventoryResponse> = inventoryRepo.getInventoryByShop(shopId, limit, offset)
}
