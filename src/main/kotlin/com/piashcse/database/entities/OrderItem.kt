package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object OrderItemTable : BaseIntIdTable("order_item") {
    val orderId = reference("order_id", OrderTable.id)
    val productId = reference("product_id", ProductTable.id)
    val quantity = integer("quantity")
    val price = decimal("price", 10, 2) // Price of the product at the time of the order
}

class OrderItemDAO(id: EntityID<String>) : BaseIntEntity(id, OrderItemTable) {
    companion object : BaseIntEntityClass<OrderItemDAO>(OrderItemTable)

    var orderId by OrderItemTable.orderId
    var productId by OrderItemTable.productId
    var quantity by OrderItemTable.quantity
    var price by OrderItemTable.price
}