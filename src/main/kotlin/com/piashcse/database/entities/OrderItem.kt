package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal

object OrderItemTable : BaseIntIdTable("order_item") {
    val orderId = reference("order_id", OrderTable.id)
    val productId = reference("product_id", ProductTable.id)
    val shopId = reference("shop_id", ShopTable.id) // Shop that sold this item
    val quantity = integer("quantity")
    val price = decimal("price", 10, 2) // Price of the product at the time of the order
    val discountAmount = decimal("discount_amount", 10, 2).default(BigDecimal("0.00")) // Discount applied to this item
    val taxAmount = decimal("tax_amount", 10, 2).default(BigDecimal("0.00")) // Tax applied to this item
    val total = decimal("total", 10, 2) // Total for this order item (quantity * price - discount + tax)
    val sku = varchar("sku", 100).nullable() // SKU of the product at the time of order
    val productName = varchar("product_name", 255).nullable() // Name of the product at the time of order
    // createdAt and updatedAt are inherited from BaseIntIdTable
}

class OrderItemDAO(id: EntityID<String>) : BaseIntEntity(id, OrderItemTable) {
    companion object : BaseIntEntityClass<OrderItemDAO>(OrderItemTable, OrderItemDAO::class.java)

    var orderId by OrderItemTable.orderId
    var productId by OrderItemTable.productId
    var shopId by OrderItemTable.shopId
    var quantity by OrderItemTable.quantity
    var price by OrderItemTable.price
    var discountAmount by OrderItemTable.discountAmount
    var taxAmount by OrderItemTable.taxAmount
    var total by OrderItemTable.total
    var sku by OrderItemTable.sku
    var productName by OrderItemTable.productName
}