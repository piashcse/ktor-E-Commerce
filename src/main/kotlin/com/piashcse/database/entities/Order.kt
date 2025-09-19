package com.piashcse.database.entities

import com.piashcse.constants.OrderStatus
import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import com.piashcse.model.response.Order
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object OrderTable : BaseIntIdTable("order") {
    val userId = reference("user_id", UserTable.id)
    val subTotal = float("sub_total")
    val total = float("total")
    val status = enumerationByName<OrderStatus>("status", 30).clientDefault { OrderStatus.PENDING }
}

class OrderDAO(id: EntityID<String>) : BaseIntEntity(id, OrderTable) {
    companion object : BaseIntEntityClass<OrderDAO>(OrderTable, OrderDAO::class.java)

    var userId by OrderTable.userId
    var subTotal by OrderTable.subTotal
    var total by OrderTable.total
    var status by OrderTable.status
    fun response() = Order(
        id.value,
        subTotal,
        total,
        status,
    )
}