package com.piashcse.database.entities

import com.piashcse.constants.AppConstants
import com.piashcse.constants.InventoryStatus
import com.piashcse.constants.Message
import com.piashcse.constants.ProductStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.event.LowStockEvent
import com.piashcse.event.OutboxPublisher
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.jdbc.select
import java.math.BigDecimal

/** All inventory rows (warehouses) for a product in its shop, with optional row locking. */
fun ProductDAO.inventories(forUpdate: Boolean = false): List<InventoryDAO> {
    val productId = this.id
    val shopIdValue = this.shopId?.value ?: return emptyList()
    val activeWarehouses =
        WarehouseDAO.find {
            (WarehouseTable.shopId eq EntityID(shopIdValue, ShopTable)) and
                (WarehouseTable.isActive eq true)
        }.map { it.id }
    if (activeWarehouses.isEmpty()) return emptyList()
    val query =
        InventoryDAO.find {
            (InventoryTable.productId eq productId) and
                (InventoryTable.shopId eq EntityID(shopIdValue, ShopTable)) and
                (InventoryTable.warehouseId inList activeWarehouses)
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

/** One warehouse-level stock movement produced by a decrement. */
data class StockAllocation(
    val warehouseId: String,
    val quantity: Int,
)

/** Decrements stock across warehouses, oldest first, and throws if insufficient. */
fun ProductDAO.decrementStock(quantity: Int): List<StockAllocation> = decrementStockFor(null, quantity)

/** Decrements product-level stock at a specific warehouse (POS/BOPIS). */
fun ProductDAO.effectiveStockForAt(
    warehouseId: String,
    variantId: String?,
): Int =
    inventories()
        .filter { it.warehouseId.value == warehouseId && it.variantId?.value == variantId }
        .sumOf { it.stockQuantity }

fun ProductDAO.decrementStockAt(
    warehouseId: String,
    quantity: Int,
): List<StockAllocation> = decrementStockAt(warehouseId, null, quantity)

fun ProductDAO.decrementStockAt(
    warehouseId: String,
    variantId: String?,
    quantity: Int,
): List<StockAllocation> {
    val rows = inventories(forUpdate = true).filter { it.warehouseId.value == warehouseId && it.variantId?.value == variantId }
    if (rows.isEmpty()) throw ValidationException(Message.Inventory.NOT_FOUND)
    val available = rows.sumOf { it.stockQuantity }
    if (available < quantity) {
        throw ValidationException(Message.Validation.insufficientStock(name, available))
    }
    val previousStatuses = rows.associate { it.id.value to it.status }
    val allocations = mutableListOf<StockAllocation>()
    var remaining = quantity
    rows.forEach { inv ->
        if (remaining <= 0) return@forEach
        val take = minOf(inv.stockQuantity, remaining)
        if (take > 0) {
            inv.stockQuantity -= take
            inv.status = InventoryStatus.fromStockLevel(inv.stockQuantity, inv.minimumStockLevel)
            allocations += StockAllocation(inv.warehouseId.value, take)
            remaining -= take
        }
    }
    enqueueLowStock(rows, previousStatuses)
    if (deletedAt == null) {
        status = if (inventories().sumOf { it.stockQuantity } <= 0) ProductStatus.OUT_OF_STOCK else ProductStatus.ACTIVE
    }
    return allocations
}

fun ProductDAO.decrementStockFor(
    variantId: String?,
    quantity: Int,
): List<StockAllocation> {
    val allRows = inventories(forUpdate = true)
    val rows = allRows.filter { it.variantId?.value == variantId }
    if (rows.isEmpty()) throw ValidationException(Message.Inventory.NOT_FOUND)
    val available = rows.sumOf { it.stockQuantity }
    if (available < quantity) {
        throw ValidationException(Message.Validation.insufficientStock(name, available))
    }
    val previousStatuses = rows.associate { it.id.value to it.status }
    val allocations = mutableListOf<StockAllocation>()
    var remaining = quantity
    rows.forEach { inv ->
        if (remaining <= 0) return@forEach
        val take = minOf(inv.stockQuantity, remaining)
        if (take > 0) {
            inv.stockQuantity -= take
            inv.status = InventoryStatus.fromStockLevel(inv.stockQuantity, inv.minimumStockLevel)
            allocations += StockAllocation(inv.warehouseId.value, take)
            remaining -= take
        }
    }
    enqueueLowStock(rows, previousStatuses)
    syncProductStatus(allRows.sumOf { it.stockQuantity })
    return allocations
}

private fun ProductDAO.enqueueLowStock(
    rows: List<InventoryDAO>,
    previousStatuses: Map<String, InventoryStatus>,
) {
    rows.filter { it.status.needsAttention && previousStatuses[it.id.value] != it.status }
        .forEach { inventory ->
            OutboxPublisher.enqueueTx(
                "PRODUCT",
                id.value,
                LowStockEvent(
                    productId = id.value,
                    variantId = inventory.variantId?.value,
                    shopId = shopId?.value,
                    stockQuantity = inventory.stockQuantity,
                ),
            )
        }
}

private fun ProductDAO.syncProductStatus(stock: Int) {
    if (deletedAt != null) return
    status = if (stock <= 0) ProductStatus.OUT_OF_STOCK else ProductStatus.ACTIVE
}

/** Re-derive product availability after an inventory write (restocks included). */
fun ProductDAO.refreshProductStatus() {
    syncProductStatus(inventories().sumOf { it.stockQuantity })
}

/** Restores stock to the oldest (default) warehouse after cancellation. */
fun ProductDAO.restoreStock(quantity: Int) = restoreStockFor(null, quantity)

fun ProductDAO.restoreStockFor(
    variantId: String?,
    quantity: Int,
    warehouseId: String? = null,
) {
    val allRows = inventories(forUpdate = true)
    val matching = allRows.filter { it.variantId?.value == variantId }
    val inv =
        (
            warehouseId?.let { id -> matching.firstOrNull { it.warehouseId.value == id } }
                ?: matching.firstOrNull()
        ) ?: throw ValidationException(Message.Inventory.NOT_FOUND)
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

/**
 * Aggregated availability for many products in a single query. Used by catalog
 * cards so stock badges never trigger an N+1 query per row.
 */
private fun activeWarehouseIds() = WarehouseTable.select(WarehouseTable.id).where { WarehouseTable.isActive eq true }

fun stockByProductIds(productIds: Collection<String>): Map<String, Int> {
    if (productIds.isEmpty()) return emptyMap()
    return InventoryDAO.find {
        (InventoryTable.productId inList productIds.distinct().map { it.entityID(ProductTable) }) and
            (InventoryTable.warehouseId inSubQuery activeWarehouseIds())
    }.groupBy { it.productId.value }.mapValues { (_, rows) -> rows.sumOf { it.stockQuantity } }
}

/** Aggregated availability per variant in a single query. */
fun stockByVariantIds(variantIds: Collection<String>): Map<String, Int> {
    if (variantIds.isEmpty()) return emptyMap()
    return InventoryDAO.find {
        (InventoryTable.variantId inList variantIds.distinct().map { it.entityID(ProductVariantTable) }) and
            (InventoryTable.warehouseId inSubQuery activeWarehouseIds())
    }
        .mapNotNull { row -> row.variantId?.value?.let { it to row.stockQuantity } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, quantities) -> quantities.sum() }
}
