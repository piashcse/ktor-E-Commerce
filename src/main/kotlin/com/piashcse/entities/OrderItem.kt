package com.piashcse.entities

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object OrderItemTable : BaseIntIdTable("order_item") {
    val orderId = reference("order_id", OrderTable.id)
    val productId = reference("product_id", ProductTable.id)
    val quantity = integer("quantity")
}

class OrderItemDAO(id: EntityID<String>) : BaseIntEntity(id, OrderItemTable) {
    companion object : BaseIntEntityClass<OrderItemDAO>(OrderItemTable)

    var orderId by OrderItemTable.orderId
    var productId by OrderItemTable.productId
    var quantity by OrderItemTable.quantity
}