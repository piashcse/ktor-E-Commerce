package com.example.entities.orders

import com.example.entities.product.ProductTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object OrderDetailTable : IdTable<String>("orders") {
    override val id: Column<EntityID<String>> = com.example.entities.shop.ShopTable.text("id").uniqueIndex().entityId()
    val order_id = reference("order_id", OrdersTable.id)
    val product_id = reference("product_id", ProductTable.id)
    val price = float("price")
    val city = text("city")
    val sku = text("order_email")
    val quantity = float("quantity")
    override val primaryKey = PrimaryKey(id)
}

class OrderDetailEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, OrderDetailEntity>(OrdersTable)
    var order_id by OrderDetailTable.order_id
    var product_id by OrderDetailTable.product_id
    var price by OrderDetailTable.price
    var city by OrderDetailTable.city
    var sku by OrderDetailTable.sku
    var quantity by OrderDetailTable.quantity
    // fun shopResponse() = Shop(id.value, shop_name)
}