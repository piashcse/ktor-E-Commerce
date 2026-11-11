package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import java.math.BigDecimal

object OrderItemTable : BaseIdTable("order_item") {
    val orderId = reference("order_id", OrderTable.id)
    val productId = reference("product_id", ProductTable.id)
    val variantId = reference("variant_id", ProductVariantTable.id).nullable() // Variant at the time of order
    val shopId = reference("shop_id", ShopTable.id) // Shop that sold this item
    val quantity = integer("quantity")
    val price = decimal("price", 10, 2) // Price of the product at the time of the order
    val discountAmount = decimal("discount_amount", 10, 2).default(BigDecimal("0.00")) // Discount applied to this item
    val taxAmount = decimal("tax_amount", 10, 2).default(BigDecimal("0.00")) // Tax applied to this item
    val taxRate = decimal("tax_rate", 6, 4).default(BigDecimal.ZERO)
    val taxInclusive = bool("tax_inclusive").default(false)
    val taxClassId = reference("tax_class_id", TaxClassTable.id).nullable()
    val isDigital = bool("is_digital").default(false)
    val total = decimal("total", 10, 2) // Total for this order item (quantity * price - discount + tax)
    val sku = varchar("sku", 100).nullable() // SKU of the product at the time of order
    val productName = varchar("product_name", 255).nullable() // Name of the product at the time of order
    val variantName = varchar("variant_name", 255).nullable() // Variant label at the time of order
    // createdAt and updatedAt are inherited from BaseIdTable
}

class OrderItemDAO(id: EntityID<String>) : BaseEntity(id, OrderItemTable) {
    companion object : BaseEntityClass<OrderItemDAO>(OrderItemTable, OrderItemDAO::class.java) {
        fun itemsForOrder(orderId: EntityID<String>) = find { OrderItemTable.orderId eq orderId }.toList()

        fun itemsForOrders(orderIds: List<EntityID<String>>): Map<String, List<OrderItemDAO>> =
            find { OrderItemTable.orderId inList orderIds }.groupBy { it.orderId.value }
    }

    var orderId by OrderItemTable.orderId
    var productId by OrderItemTable.productId
    var variantId by OrderItemTable.variantId
    var shopId by OrderItemTable.shopId
    var quantity by OrderItemTable.quantity
    var price by OrderItemTable.price
    var discountAmount by OrderItemTable.discountAmount
    var taxAmount by OrderItemTable.taxAmount
    var taxRate by OrderItemTable.taxRate
    var taxInclusive by OrderItemTable.taxInclusive
    var taxClassId by OrderItemTable.taxClassId
    var isDigital by OrderItemTable.isDigital
    var total by OrderItemTable.total
    var sku by OrderItemTable.sku
    var productName by OrderItemTable.productName
    var variantName by OrderItemTable.variantName
}
