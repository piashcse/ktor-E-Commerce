package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object OrderTable : BaseIntIdTable("order") {
    val userId = reference("user_id", UserTable.id)
    val subTotal = float("sub_total")
    val total = float("total")
    val status = enumerationByName("status", 30, OrderStatus::class).clientDefault { OrderStatus.PENDING }

    enum class OrderStatus {
        PENDING, CONFIRMED, PAID, DELIVERED, CANCELED, RECEIVED
    }
}

class OrderDAO(id: EntityID<String>) : BaseIntEntity(id, OrderTable) {
    companion object : BaseIntEntityClass<OrderDAO>(OrderTable)

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

data class Order(
    val orderId: String,
    val subTotal: Float,
    val total: Float,
    val status: OrderTable.OrderStatus,
)