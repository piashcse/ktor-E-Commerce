package com.piashcse.database.entities

import com.piashcse.constants.OrderStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object OrderStatusHistoryTable : BaseIdTable("order_status_history") {
    val orderId = reference("order_id", OrderTable.id)
    val status = enumerationByName<OrderStatus>("status", 30)
    val notes = text("notes").nullable()
    val changedBy = reference("changed_by", UserTable.id).nullable()
    // createdAt is inherited from BaseIdTable
}

class OrderStatusHistoryDAO(id: EntityID<String>) : BaseEntity(id, OrderStatusHistoryTable) {
    companion object : BaseEntityClass<OrderStatusHistoryDAO>(OrderStatusHistoryTable, OrderStatusHistoryDAO::class.java)

    var orderId by OrderStatusHistoryTable.orderId
    var status by OrderStatusHistoryTable.status
    var notes by OrderStatusHistoryTable.notes
    var changedBy by OrderStatusHistoryTable.changedBy
}
