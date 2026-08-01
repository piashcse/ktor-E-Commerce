package com.piashcse.feature.inventory

import com.piashcse.constants.InventoryStatus
import com.piashcse.constants.Message
import com.piashcse.constants.StockOperation
import com.piashcse.constants.UserType
import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.response.InventoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.requireNotBlank
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException

class InventoryService(private val inventoryRepo: InventoryRepository) {

    /**
     * Updates inventory stock, validating ownership, the operation and quantity
     * and enforcing the insufficient-stock and maximum-stock rules. Runs under
     * a row lock in a retryable transaction.
     */
    suspend fun updateStock(
        userId: String,
        productId: String,
        quantity: Int,
        operation: String = "add",
    ): InventoryResponse = suspendRetryQuery {
        val shopId = inventoryRepo.getInventoryShopId(productId)
            ?: productId.throwNotFound("Inventory")
        requireShopAccess(userId, shopId)

        val inventory = inventoryRepo.getInventoryForUpdate(productId)
        val op = operation.parseEnum<StockOperation>("operation")

        val isSet = op == StockOperation.SET
        if (quantity < 0 || (!isSet && quantity == 0))
            throw ValidationException(Message.Inventory.quantityNotPositive(operation))

        val newStock = when (op) {
            StockOperation.ADD -> inventory.stockQuantity + quantity
            StockOperation.SUBTRACT -> {
                if (inventory.stockQuantity < quantity)
                    throw ValidationException(Message.Inventory.insufficientStock(inventory.stockQuantity, quantity))
                inventory.stockQuantity - quantity
            }
            StockOperation.SET -> quantity
        }
        if (newStock > inventory.maximumStockLevel)
            throw ValidationException(Message.Inventory.maxStockExceeded(inventory.maximumStockLevel))

        val status = InventoryStatus.fromStockLevel(newStock, inventory.minimumStockLevel)
        inventoryRepo.setStock(productId, newStock, status)
    }

    /**
     * Creates or updates inventory for a product, validating authorization and
     * request inputs. Runs in a retryable transaction.
     */
    suspend fun createOrUpdateInventory(
        userId: String,
        inventoryRequest: InventoryRequest,
    ): InventoryResponse = suspendRetryQuery {
        inventoryRequest.productId.requireNotBlank("Product ID")
        inventoryRequest.shopId.requireNotBlank("Shop ID")
        if (inventoryRequest.stockQuantity < 0) throw ValidationException(Message.Inventory.NEGATIVE_STOCK)
        if (inventoryRequest.minimumStockLevel != null && inventoryRequest.minimumStockLevel < 0)
            throw ValidationException(Message.Validation.negativeValue("Minimum stock level"))
        if (inventoryRequest.maximumStockLevel != null && inventoryRequest.maximumStockLevel < 0)
            throw ValidationException(Message.Validation.negativeValue("Maximum stock level"))

        requireShopAccess(userId, inventoryRequest.shopId)
        val productShopId = inventoryRepo.getProductShopId(inventoryRequest.productId)
        if (productShopId != inventoryRequest.shopId)
            throw ValidationException(Message.Inventory.PRODUCT_SHOP_MISMATCH)

        val min = inventoryRequest.minimumStockLevel
        val max = inventoryRequest.maximumStockLevel
        if (min != null && max != null && min > max)
            throw ValidationException(Message.Validation.negativeValue("Minimum stock level"))

        inventoryRepo.createOrUpdateInventory(inventoryRequest)
    }

    suspend fun getInventoryByProduct(
        userId: String,
        userType: UserType,
        productId: String,
    ): InventoryResponse? {
        val shopId = inventoryRepo.getInventoryShopId(productId)
            ?: return null
        requireShopAccess(userId, userType, shopId)
        return inventoryRepo.getInventoryByProduct(productId)
    }

    suspend fun getLowStockProducts(
        userId: String,
        userType: UserType,
        limit: Int = 10,
        offset: Int = 0,
    ): PaginatedResponse<InventoryResponse> {
        val shopId = if (userType.isAdminOrHigher) {
            null
        } else {
            val sellerShopId = inventoryRepo.getSellerShopId(userId)
            if (sellerShopId == null) {
                return PaginatedResponse.of(emptyList(), 0, limit, offset)
            }
            requireShopAccess(userId, userType, sellerShopId)
            sellerShopId
        }
        return inventoryRepo.getLowStockProducts(shopId, limit, offset)
    }

    suspend fun getInventoryByShop(
        userId: String,
        userType: UserType,
        shopId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<InventoryResponse> {
        requireShopAccess(userId, userType, shopId)
        return inventoryRepo.getInventoryByShop(shopId, limit, offset)
    }

    private suspend fun requireShopAccess(userId: String, shopId: String) {
        val access = inventoryRepo.getInventoryAccess(userId, shopId)
        if (!access.isSellerOwner && !access.isAdmin) throw ForbiddenException(Message.Inventory.NOT_SHOP_OWNER)
    }

    private suspend fun requireShopAccess(userId: String, userType: UserType, shopId: String) {
        if (userType.isAdminOrHigher) return
        val access = inventoryRepo.getInventoryAccess(userId, shopId)
        if (!access.isSellerOwner) throw ForbiddenException(Message.Inventory.NOT_SHOP_OWNER)
    }
}
