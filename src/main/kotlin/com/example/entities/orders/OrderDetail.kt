package com.example.entities.orders

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import com.example.entities.product.ProductTable
import org.jetbrains.exposed.dao.id.EntityID

object OrderDetailsTable : BaseIntIdTable("orders") {
    val orderId = reference("order_id", OrdersTable.id)
    val productId = reference("product_id", ProductTable.id)
    val totalPrice = float("total_price")
    val sku = text("order_email")
    val quantity = float("quantity")
}

class OrderDetailEntity(id: EntityID<String>) : BaseIntEntity(id, OrdersTable) {
    companion object : BaseIntEntityClass<OrderDetailEntity>(OrdersTable)

    var orderId by OrderDetailsTable.orderId
    var productId by OrderDetailsTable.productId
    var totalPrice by OrderDetailsTable.totalPrice
    var sku by OrderDetailsTable.sku
    var quantity by OrderDetailsTable.quantity
    // fun shopResponse() = Shop(id.value, shop_name)
}