package com.piashcse.feature.inventory

import com.piashcse.constants.InventoryStatus
import com.piashcse.constants.Message
import com.piashcse.database.entities.*
import com.piashcse.event.BackInStockEvent
import com.piashcse.event.OutboxPublisher
import com.piashcse.mapper.toInventoryResponse
import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.response.InventoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class InventoryRepositoryImpl : InventoryRepository {
    companion object {
        private const val DEFAULT_MIN_STOCK = 10
        private const val DEFAULT_MAX_STOCK = 1000
    }

    private fun requireSellerShopId(actorUserId: String): String =
        requireSellerByUserId(actorUserId).shopId?.value
            ?: throw ForbiddenException(Message.Orders.NO_SHOP_ASSOCIATED)

    private fun requireOwnedProduct(
        actorUserId: String,
        productId: String,
    ): ProductDAO {
        val shopId = requireSellerShopId(actorUserId)
        val product = ProductDAO.findById(productId) ?: productId.throwNotFound("Product")
        if (product.shopId?.value != shopId) throw ForbiddenException(Message.Errors.FORBIDDEN)
        return product
    }

    private fun requireVariant(
        productId: String,
        variantId: String?,
    ): ProductVariantDAO? {
        if (variantId == null) return null
        val variant = ProductVariantDAO.findById(variantId) ?: variantId.throwNotFound("Variant")
        if (variant.productId.value != productId) throw ValidationException("Variant does not belong to this product")
        return variant
    }

    private fun variantOp(variantId: String?) =
        if (variantId == null) {
            InventoryTable.variantId.isNull()
        } else {
            InventoryTable.variantId eq variantId.entityID(ProductVariantTable)
        }

    private fun resolveWarehouse(
        shopId: String,
        warehouseId: String?,
    ): WarehouseDAO {
        if (warehouseId != null) {
            val warehouse = WarehouseDAO.findById(warehouseId) ?: warehouseId.throwNotFound("Warehouse")
            if (warehouse.shopId.value != shopId) throw ForbiddenException(Message.Errors.FORBIDDEN)
            if (!warehouse.isActive) throw ValidationException("Warehouse is inactive")
            return warehouse
        }
        return WarehouseDAO.find { WarehouseTable.shopId eq shopId.entityID(ShopTable) }
            .firstOrNull()
            ?: WarehouseDAO.new {
                this.shopId = shopId.entityID(ShopTable)
                name = "Default"
                code = "DEFAULT"
            }
    }

    private fun enqueueBackInStock(
        productId: String,
        variantId: String?,
        previousStock: Int,
        newStock: Int,
    ) {
        if (previousStock <= 0 && newStock > 0) {
            OutboxPublisher.enqueueTx("PRODUCT", productId, BackInStockEvent(productId, variantId))
        }
    }

    override suspend fun createOrUpdateInventory(
        inventoryRequest: InventoryRequest,
        actorUserId: String,
    ): InventoryResponse =
        query {
            inventoryRequest.productId.requireNotBlank("Product ID")
            inventoryRequest.shopId.requireNotBlank("Shop ID")
            if (inventoryRequest.stockQuantity < 0) throw ValidationException(Message.Inventory.NEGATIVE_STOCK)
            if (inventoryRequest.minimumStockLevel != null && inventoryRequest.minimumStockLevel < 0) {
                throw ValidationException(Message.Validation.negativeValue("Minimum stock level"))
            }
            if (inventoryRequest.maximumStockLevel != null && inventoryRequest.maximumStockLevel < 0) {
                throw ValidationException(Message.Validation.negativeValue("Maximum stock level"))
            }
            if (inventoryRequest.maximumStockLevel != null &&
                inventoryRequest.maximumStockLevel < inventoryRequest.stockQuantity
            ) {
                throw ValidationException("Maximum stock level cannot be below the current stock quantity")
            }

            val sellerShopId = requireSellerShopId(actorUserId)
            if (inventoryRequest.shopId != sellerShopId) throw ForbiddenException(Message.Errors.FORBIDDEN)

            val product = ProductDAO.findById(inventoryRequest.productId) ?: inventoryRequest.productId.throwNotFound("Product")
            if (product.shopId?.value != sellerShopId) throw ForbiddenException(Message.Errors.FORBIDDEN)
            val variant = requireVariant(product.id.value, inventoryRequest.variantId)
            val warehouse = resolveWarehouse(sellerShopId, inventoryRequest.warehouseId)

            val existing =
                InventoryDAO.find {
                    (InventoryTable.productId eq inventoryRequest.productId) and
                        (InventoryTable.shopId eq inventoryRequest.shopId) and
                        (InventoryTable.warehouseId eq warehouse.id) and
                        variantOp(inventoryRequest.variantId)
                }.firstOrNull()

            val previousStock = existing?.stockQuantity ?: 0
            val inventory =
                existing?.apply {
                    stockQuantity = inventoryRequest.stockQuantity
                    minimumStockLevel = inventoryRequest.minimumStockLevel ?: minimumStockLevel
                    maximumStockLevel = inventoryRequest.maximumStockLevel ?: maximumStockLevel
                    status = InventoryStatus.fromStockLevel(stockQuantity, minimumStockLevel)
                } ?: InventoryDAO.new {
                    productId = inventoryRequest.productId.entityID(ProductTable)
                    variantId = variant?.id
                    shopId = inventoryRequest.shopId.entityID(ShopTable)
                    warehouseId = warehouse.id
                    stockQuantity = inventoryRequest.stockQuantity
                    minimumStockLevel = inventoryRequest.minimumStockLevel ?: DEFAULT_MIN_STOCK
                    maximumStockLevel = inventoryRequest.maximumStockLevel ?: DEFAULT_MAX_STOCK
                    status = InventoryStatus.fromStockLevel(inventoryRequest.stockQuantity, minimumStockLevel)
                }
            enqueueBackInStock(product.id.value, inventoryRequest.variantId, previousStock, inventory.stockQuantity)
            inventory.toInventoryResponse()
        }

    override suspend fun getInventoryByProduct(
        productId: String,
        actorUserId: String,
        variantId: String?,
    ): InventoryResponse? =
        query {
            val product = requireOwnedProduct(actorUserId, productId)
            requireVariant(product.id.value, variantId)
            InventoryDAO.find {
                (InventoryTable.productId eq productId) and variantOp(variantId)
            }.firstOrNull()?.toInventoryResponse()
        }

    override suspend fun updateStock(
        actorUserId: String,
        productId: String,
        quantity: Int,
        operation: String,
        warehouseId: String?,
        variantId: String?,
    ): InventoryResponse =
        retryQuery {
            val product = requireOwnedProduct(actorUserId, productId)
            requireVariant(product.id.value, variantId)
            product.shopId?.value?.let { resolveWarehouse(it, warehouseId) }
            val shopCondition =
                product.shopId?.let { InventoryTable.shopId eq it } ?: InventoryTable.shopId.isNull()
            val inventoryQuery =
                InventoryDAO.find {
                    (InventoryTable.productId eq product.id) and
                        shopCondition and variantOp(variantId) and
                        if (warehouseId == null) {
                            org.jetbrains.exposed.v1.core.Op.TRUE
                        } else {
                            InventoryTable.warehouseId eq warehouseId.entityID(WarehouseTable)
                        }
                }.forUpdate()
            val inventory =
                inventoryQuery.orderBy(InventoryTable.createdAt to SortOrder.ASC).firstOrNull()
                    ?: productId.throwNotFound("Inventory")

            if (quantity <= 0) throw ValidationException(Message.Inventory.quantityNotPositive(operation))
            if (operation.lowercase() !in listOf("add", "subtract", "set")) {
                throw ValidationException(Message.Inventory.invalidOperation(operation))
            }

            val newStock =
                when (operation.lowercase()) {
                    "add" -> inventory.stockQuantity + quantity
                    "subtract" -> {
                        if (inventory.stockQuantity < quantity) {
                            throw ValidationException(Message.Inventory.insufficientStock(inventory.stockQuantity, quantity))
                        }
                        inventory.stockQuantity - quantity
                    }
                    "set" -> quantity
                    else -> throw ValidationException(Message.Inventory.invalidOperation(operation))
                }

            if (newStock > inventory.maximumStockLevel) {
                throw ValidationException("Stock cannot exceed the maximum level ${inventory.maximumStockLevel}")
            }

            val previousStock = inventory.stockQuantity
            inventory.stockQuantity = newStock
            inventory.status = InventoryStatus.fromStockLevel(newStock, inventory.minimumStockLevel)
            enqueueBackInStock(product.id.value, inventory.variantId?.value, previousStock, newStock)
            inventory.toInventoryResponse()
        }

    override suspend fun getLowStockProducts(
        actorUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<InventoryResponse> =
        query {
            val shopId = requireSellerShopId(actorUserId)
            InventoryTable.selectAll()
                .andWhere { InventoryTable.shopId eq shopId }
                .andWhere { InventoryTable.stockQuantity lessEq InventoryTable.minimumStockLevel }
                .orderBy(InventoryTable.stockQuantity to SortOrder.ASC)
                .toPaginatedResponse(limit, offset) { InventoryDAO.wrapRow(it).toInventoryResponse() }
        }

    override suspend fun getInventoryByShop(
        actorUserId: String,
        shopId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<InventoryResponse> =
        query {
            if (requireSellerShopId(actorUserId) != shopId) throw ForbiddenException(Message.Errors.FORBIDDEN)
            InventoryTable.selectAll().andWhere { InventoryTable.shopId eq shopId }
                .toPaginatedResponse(limit, offset) { InventoryDAO.wrapRow(it).toInventoryResponse() }
        }

    override suspend fun getAllInventory(
        limit: Int,
        offset: Int,
        shopId: String?,
    ): PaginatedResponse<InventoryResponse> =
        query {
            val baseQuery = InventoryTable.selectAll()
            shopId?.let { baseQuery.andWhere { InventoryTable.shopId eq it } }
            baseQuery.toPaginatedResponse(limit, offset) { InventoryDAO.wrapRow(it).toInventoryResponse() }
        }
}
