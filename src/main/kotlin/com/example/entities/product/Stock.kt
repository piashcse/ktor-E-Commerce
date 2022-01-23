package com.example.entities.product

import com.example.entities.shop.ShopTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object StockTable : IdTable<String>("stock") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val product_id = text("product_id").references(ProductTable.id)
    val shop_id = text("shop_id").references(ShopTable.id)
    val quantity = integer("quantity") // product quantity
    override val primaryKey = PrimaryKey(id)
}

class StockEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, StockEntity>(ProductImage)

    var product_id by StockTable.product_id
    var shop_id by StockTable.shop_id
    var quantity by StockTable.quantity
}
