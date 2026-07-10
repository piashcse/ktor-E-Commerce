package com.piashcse.feature.inventory

import com.piashcse.constants.InventoryStatus
import com.piashcse.constants.Message
import com.piashcse.database.entities.*
import com.piashcse.mapper.toInventoryResponse
import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.response.InventoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireNotBlank
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class InventoryRepositoryImpl : InventoryRepository {
    companion object {
        private const val DEFAULT_MIN_STOCK = 10
        private const val DEFAULT_MAX_STOCK = 1000
    }

    override suspend fun createOrUpdateInventory(request: InventoryRequest): InventoryResponse = query {
        request.productId.requireNotBlank("Product ID")
        request.shopId.requireNotBlank("Shop ID")
        if (request.stockQuantity < 0) throw ValidationException(Message.Inventory.NEGATIVE_STOCK)
        if (request.minimumStockLevel != null && request.minimumStockLevel < 0)
            throw ValidationException(Message.Validation.negativeValue("Minimum stock level"))
        if (request.maximumStockLevel != null && request.maximumStockLevel < 0)
            throw ValidationException(Message.Validation.negativeValue("Maximum stock level"))

        ProductDAO.findById(request.productId) ?: request.productId.throwNotFound("Product")
        ShopDAO.findById(request.shopId) ?: request.shopId.throwNotFound("Shop")

        val existing = InventoryDAO.find {
            (InventoryTable.productId eq request.productId) and
                (InventoryTable.shopId eq request.shopId)
        }.firstOrNull()

        val inventory = existing?.apply {
            stockQuantity = request.stockQuantity
            minimumStockLevel = request.minimumStockLevel ?: minimumStockLevel
            maximumStockLevel = request.maximumStockLevel ?: maximumStockLevel
            status = InventoryStatus.fromStockLevel(stockQuantity, minimumStockLevel)
        } ?: InventoryDAO.new {
            productId = EntityID(request.productId, ProductTable)
            shopId = EntityID(request.shopId, ShopTable)
            stockQuantity = request.stockQuantity
            minimumStockLevel = request.minimumStockLevel ?: DEFAULT_MIN_STOCK
            maximumStockLevel = request.maximumStockLevel ?: DEFAULT_MAX_STOCK
            status = InventoryStatus.fromStockLevel(request.stockQuantity, minimumStockLevel)
        }
        inventory.toInventoryResponse()
    }

    override suspend fun getInventoryByProduct(productId: String): InventoryResponse? = query {
        InventoryDAO.find { InventoryTable.productId eq productId }.firstOrNull()?.toInventoryResponse()
    }

    override suspend fun updateStock(
        productId: String,
        quantity: Int,
        operation: String,
    ): InventoryResponse = query {
        val inventory = InventoryDAO.find { InventoryTable.productId eq productId }.firstOrNull()
            ?: productId.throwNotFound("Inventory")

        if (quantity <= 0) throw ValidationException(Message.Inventory.quantityNotPositive(operation))
        if (operation.lowercase() !in listOf("add", "subtract", "set"))
            throw ValidationException(Message.Inventory.invalidOperation(operation))

        val newStock = when (operation.lowercase()) {
            "add" -> inventory.stockQuantity + quantity
            "subtract" -> {
                if (inventory.stockQuantity < quantity)
                    throw ValidationException(Message.Inventory.insufficientStock(inventory.stockQuantity, quantity))
                inventory.stockQuantity - quantity
            }
            "set" -> {
                if (quantity < 0) throw ValidationException(Message.Inventory.NEGATIVE_QUANTITY)
                quantity
            }
            else -> throw ValidationException(Message.Inventory.invalidOperation(operation))
        }

        InventoryTable.update({ InventoryTable.id eq inventory.id }) {
            it[stockQuantity] = newStock
        }

        inventory.apply {
            stockQuantity = newStock
            status = InventoryStatus.fromStockLevel(newStock, minimumStockLevel)
        }.toInventoryResponse()
    }

    override suspend fun getLowStockProducts(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<InventoryResponse> = query {
        InventoryTable.selectAll().andWhere { InventoryTable.stockQuantity lessEq InventoryTable.minimumStockLevel }
            .orderBy(InventoryTable.stockQuantity to SortOrder.ASC)
            .toPaginatedResponse(limit, offset) { InventoryDAO.wrapRow(it).toInventoryResponse() }
    }

    override suspend fun getInventoryByShop(
        shopId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<InventoryResponse> = query {
        InventoryTable.selectAll().andWhere { InventoryTable.shopId eq shopId }
            .toPaginatedResponse(limit, offset) { InventoryDAO.wrapRow(it).toInventoryResponse() }
    }
}
