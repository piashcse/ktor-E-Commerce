package com.piashcse.database.entities

import com.piashcse.constants.AppConstants
import com.piashcse.constants.InventoryStatus
import com.piashcse.constants.Message
import com.piashcse.constants.ProductStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.utils.common.Money
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal

/** All inventory rows (warehouses) for a product in its shop, with optional row locking. */
fun ProductDAO.inventories(forUpdate: Boolean = false): List<InventoryDAO> {
    val productId = this.id
    val shopIdValue = this.shopId?.value ?: return emptyList()
    val query =
        InventoryDAO.find {
            (InventoryTable.productId eq productId) and (InventoryTable.shopId eq EntityID(shopIdValue, ShopTable))
        }
    return (if (forUpdate) query.forUpdate() else query)
        .orderBy(InventoryTable.createdAt to org.jetbrains.exposed.v1.core.SortOrder.ASC)
        .toList()
}

/** Inventory rows for a single variant across the product's warehouses. */
fun ProductDAO.variantInventories(
    variantId: String,
    forUpdate: Boolean = false,
): List<InventoryDAO> = inventories(forUpdate = forUpdate).filter { it.variantId?.value == variantId }

/** Returns effective stock across all warehouses (single source of truth). */
fun ProductDAO.effectiveStock(forUpdate: Boolean = false): Int =
    inventories(forUpdate = forUpdate).filter { it.variantId == null }.sumOf { it.stockQuantity }

/** Effective stock for a variant, or product-level stock when [variantId] is null. */
fun ProductDAO.effectiveStockFor(
    variantId: String?,
    forUpdate: Boolean = false,
): Int =
    if (variantId == null) {
        effectiveStock(forUpdate = forUpdate)
    } else {
        variantInventories(variantId, forUpdate = forUpdate).sumOf { it.stockQuantity }
    }

/** Decrements stock across warehouses, oldest first, and throws if insufficient. */
fun ProductDAO.decrementStock(quantity: Int) = decrementStockFor(null, quantity)

/** Decrements product-level stock at a specific warehouse (POS/BOPIS). */
fun ProductDAO.decrementStockAt(
    warehouseId: String,
    quantity: Int,
) {
    val rows = inventories(forUpdate = true).filter { it.warehouseId.value == warehouseId && it.variantId == null }
    if (rows.isEmpty()) throw ValidationException(Message.Inventory.NOT_FOUND)
    val available = rows.sumOf { it.stockQuantity }
    if (available < quantity) {
        throw ValidationException(Message.Validation.insufficientStock(name, available))
    }
    var remaining = quantity
    rows.forEach { inv ->
        if (remaining <= 0) return@forEach
        val take = minOf(inv.stockQuantity, remaining)
        inv.stockQuantity -= take
        inv.status = InventoryStatus.fromStockLevel(inv.stockQuantity, inv.minimumStockLevel)
        remaining -= take
    }
    if (deletedAt == null) {
        status = if (inventories().sumOf { it.stockQuantity } <= 0) ProductStatus.OUT_OF_STOCK else ProductStatus.ACTIVE
    }
}

fun ProductDAO.decrementStockFor(
    variantId: String?,
    quantity: Int,
) {
    val allRows = inventories(forUpdate = true)
    val rows = allRows.filter { it.variantId?.value == variantId }
    if (rows.isEmpty()) throw ValidationException(Message.Inventory.NOT_FOUND)
    val available = rows.sumOf { it.stockQuantity }
    if (available < quantity) {
        throw ValidationException(Message.Validation.insufficientStock(name, available))
    }
    var remaining = quantity
    rows.forEach { inv ->
        if (remaining <= 0) return@forEach
        val take = minOf(inv.stockQuantity, remaining)
        inv.stockQuantity -= take
        inv.status = InventoryStatus.fromStockLevel(inv.stockQuantity, inv.minimumStockLevel)
        remaining -= take
    }
    syncProductStatus(allRows.sumOf { it.stockQuantity })
}

private fun ProductDAO.syncProductStatus(stock: Int) {
    if (deletedAt != null) return
    status = if (stock <= 0) ProductStatus.OUT_OF_STOCK else ProductStatus.ACTIVE
}

/** Restores stock to the oldest (default) warehouse after cancellation. */
fun ProductDAO.restoreStock(quantity: Int) = restoreStockFor(null, quantity)

fun ProductDAO.restoreStockFor(
    variantId: String?,
    quantity: Int,
) {
    val allRows = inventories(forUpdate = true)
    val inv =
        allRows.filter { it.variantId?.value == variantId }.firstOrNull()
            ?: throw ValidationException(Message.Inventory.NOT_FOUND)
    inv.stockQuantity += quantity
    inv.status = InventoryStatus.fromStockLevel(inv.stockQuantity, inv.minimumStockLevel)
    syncProductStatus(allRows.sumOf { it.stockQuantity })
}

/** Calculates commission for an order subtotal. */
fun SellerDAO.calcCommission(orderSubTotal: BigDecimal): BigDecimal = Money.commission(orderSubTotal, commissionRate)

/** Records sales for a product and promotes it to best-seller once a threshold is crossed. */
fun ProductDAO.addSales(quantity: Int) {
    totalSales = totalSales + quantity
    if (totalSales >= AppConstants.Products.BEST_SELLER_THRESHOLD) {
        bestSeller = true
    }
}

/** Reverses recorded sales (e.g. on order cancellation) and demotes the product if it falls below the threshold. */
fun ProductDAO.removeSales(quantity: Int) {
    totalSales = (totalSales - quantity).coerceAtLeast(0)
    if (totalSales < AppConstants.Products.BEST_SELLER_THRESHOLD) {
        bestSeller = false
    }
}

object InventoryTable : BaseIdTable("inventory") {
    val productId = reference("product_id", ProductTable.id)
    val variantId = reference("variant_id", ProductVariantTable.id).nullable()
    val shopId = reference("shop_id", ShopTable.id) // Link inventory to specific shop
    val warehouseId = reference("warehouse_id", WarehouseTable.id).index()
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
    var variantId by InventoryTable.variantId
    var shopId by InventoryTable.shopId
    var warehouseId by InventoryTable.warehouseId
    var stockQuantity by InventoryTable.stockQuantity
    var reservedQuantity by InventoryTable.reservedQuantity
    var minimumStockLevel by InventoryTable.minimumStockLevel
    var maximumStockLevel by InventoryTable.maximumStockLevel
    var status by InventoryTable.status
    var lastRestocked by InventoryTable.lastRestocked
}
