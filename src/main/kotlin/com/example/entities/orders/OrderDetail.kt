package com.example.entities.orders

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import com.example.entities.product.ProductTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object OrderDetailTable : BaseIntIdTable("orders") {
    val orderId = reference("order_id", OrdersTable.id)
    val productId = reference("product_id", ProductTable.id)
    val price = float("price")
    val city = text("city")
    val sku = text("order_email")
    val quantity = float("quantity")
}

class OrderDetailEntity(id: EntityID<String>) : BaseIntEntity(id, OrdersTable) {
    companion object : BaseIntEntityClass<OrderDetailEntity>(OrdersTable)

    var orderId by OrderDetailTable.orderId
    var productId by OrderDetailTable.productId
    var price by OrderDetailTable.price
    var city by OrderDetailTable.city
    var sku by OrderDetailTable.sku
    var quantity by OrderDetailTable.quantity
    // fun shopResponse() = Shop(id.value, shop_name)
}