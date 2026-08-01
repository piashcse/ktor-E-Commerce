package com.piashcse.feature.inventory

import com.piashcse.constants.AppConstants.Inventory.DEFAULT_MAX_STOCK
import com.piashcse.constants.AppConstants.Inventory.DEFAULT_MIN_STOCK
import com.piashcse.constants.InventoryStatus
import com.piashcse.database.entities.*
import com.piashcse.mapper.toInventoryResponse
import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.response.InventoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class InventoryRepositoryImpl : InventoryRepository {

    override suspend fun getInventoryForUpdate(productId: String): InventoryStockInfo = query {
        val inventory = InventoryDAO.find { InventoryTable.productId eq productId }.forUpdate().firstOrNull()
            ?: productId.throwNotFound("Inventory")
        InventoryStockInfo(
            productId = inventory.productId.value,
            stockQuantity = inventory.stockQuantity,
            minimumStockLevel = inventory.minimumStockLevel,
        )
    }

    override suspend fun setStock(
        productId: String,
        newStock: Int,
        status: InventoryStatus,
    ): InventoryResponse = query {
        val inventory = InventoryDAO.find { InventoryTable.productId eq productId }.firstOrNull()
            ?: productId.throwNotFound("Inventory")
        inventory.stockQuantity = newStock
        inventory.status = status
        inventory.toInventoryResponse()
    }

    override suspend fun createOrUpdateInventory(request: InventoryRequest): InventoryResponse = query {
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
            productId = request.productId.entityID(ProductTable)
            shopId = request.shopId.entityID(ShopTable)
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
