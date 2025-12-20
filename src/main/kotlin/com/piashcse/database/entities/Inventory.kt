package com.piashcse.database.entities

import com.piashcse.constants.InventoryStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.InventoryResponse
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object InventoryTable : BaseIdTable("inventory") {
    val productId = reference("product_id", ProductTable.id)
    val shopId = reference("shop_id", ShopTable.id) // Link inventory to specific shop
    val stockQuantity = integer("stock_quantity").default(0)
    val reservedQuantity = integer("reserved_quantity").default(0) // Quantity reserved but not yet sold
    val minimumStockLevel = integer("minimum_stock_level").default(10) // When to trigger low stock alert
    val maximumStockLevel = integer("maximum_stock_level").default(1000) // Maximum stock level for the product
    val status = enumerationByName<InventoryStatus>("status", 50).default(InventoryStatus.IN_STOCK)
    val lastRestocked = datetime("last_restocked").nullable()
    // createdAt and updatedAt are inherited from BaseIdTable, so we don't need to redeclare them
}

class InventoryDAO(id: EntityID<String>) : BaseEntity(id, InventoryTable) {
    companion object : BaseEntityClass<InventoryDAO>(InventoryTable, InventoryDAO::class.java)

    var productId by InventoryTable.productId
    var shopId by InventoryTable.shopId
    var stockQuantity by InventoryTable.stockQuantity
    var reservedQuantity by InventoryTable.reservedQuantity
    var minimumStockLevel by InventoryTable.minimumStockLevel
    var maximumStockLevel by InventoryTable.maximumStockLevel
    var status by InventoryTable.status
    var lastRestocked by InventoryTable.lastRestocked

    fun response() = InventoryResponse(
        id = id.value,
        productId = productId.value,
        shopId = shopId.value,
        stockQuantity = stockQuantity,
        reservedQuantity = reservedQuantity,
        minimumStockLevel = minimumStockLevel,
        maximumStockLevel = maximumStockLevel,
        status = status,
        lastRestocked = lastRestocked,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}