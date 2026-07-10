package com.piashcse.database.entities

import com.piashcse.constants.InventoryStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal
import java.math.RoundingMode

/** Finds the inventory record for a product in its shop, with optional for-update locking. */
fun ProductDAO.findInventory(forUpdate: Boolean = false): InventoryDAO? {
    val productId = this.id
    val shopIdValue = this.shopId?.value ?: return null
    val query = InventoryDAO.find {
        (InventoryTable.productId eq productId) and (InventoryTable.shopId eq EntityID(shopIdValue, ShopTable))
    }
    return (if (forUpdate) query.forUpdate() else query).firstOrNull()
}

/** Returns effective stock: inventory stock if exists, otherwise product stock. */
fun ProductDAO.effectiveStock(forUpdate: Boolean = false): Int {
    val inv = findInventory(forUpdate = forUpdate)
    if (inv != null) return inv.stockQuantity
    if (forUpdate) ProductDAO.findById(this.id.value)?.let { return it.stockQuantity }
    return stockQuantity
}

/** Decrements effective stock (inventory if exists, else product) and updates inventory status. */
fun ProductDAO.decrementStock(quantity: Int) {
    val inv = findInventory(forUpdate = true)
    if (inv != null) {
        val newStock = (inv.stockQuantity - quantity).coerceAtLeast(0)
        inv.stockQuantity = newStock
        inv.status = InventoryStatus.fromStockLevel(newStock, inv.minimumStockLevel)
    } else {
        val lockedProduct = ProductDAO.find { ProductTable.id eq id }.forUpdate().firstOrNull()
        if (lockedProduct != null) {
            lockedProduct.stockQuantity = (lockedProduct.stockQuantity - quantity).coerceAtLeast(0)
        }
    }
}

/** Restores effective stock after cancellation. */
fun ProductDAO.restoreStock(quantity: Int) {
    val inv = findInventory(forUpdate = true)
    if (inv != null) {
        val newStock = inv.stockQuantity + quantity
        inv.stockQuantity = newStock
        inv.status = InventoryStatus.fromStockLevel(newStock, inv.minimumStockLevel)
    } else {
        val lockedProduct = ProductDAO.find { ProductTable.id eq id }.forUpdate().firstOrNull()
        if (lockedProduct != null) {
            lockedProduct.stockQuantity = lockedProduct.stockQuantity + quantity
        }
    }
}

/** Calculates commission for an order subtotal. */
fun SellerDAO.calcCommission(orderSubTotal: BigDecimal): BigDecimal =
    orderSubTotal.multiply(commissionRate).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)

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

    init {
        uniqueIndex("inventory_product_shop_idx", productId, shopId)
    }
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

}
