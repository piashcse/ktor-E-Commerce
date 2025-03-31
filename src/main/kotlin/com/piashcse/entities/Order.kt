package com.piashcse.entities

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object OrderTable : BaseIntIdTable("order") {
    val userId = reference("user_id", UserTable.id)
    val quantity = integer("quantity") // total number of items
    val subTotal = float("sub_total")
    val total = float("total")
    val shippingCharge = float("shopping_charge").clientDefault { 0.0f }
    val vat = float("vat").nullable()
    val status = enumerationByName("status", 30, OrderStatus::class).clientDefault { OrderStatus.PENDING }

    enum class OrderStatus {
        PENDING, CONFIRMED, PAID, DELIVERED, CANCELED, RECEIVED
    }
}

class OrderDAO(id: EntityID<String>) : BaseIntEntity(id, OrderTable) {
    companion object : BaseIntEntityClass<OrderDAO>(OrderTable)

    var userId by OrderTable.userId
    var quantity by OrderTable.quantity
    var subTotal by OrderTable.subTotal
    var total by OrderTable.total
    var shippingCharge by OrderTable.shippingCharge
    var vat by OrderTable.vat
    var status by OrderTable.status
    fun response() = Order(
        id.value,
        quantity,
        subTotal,
        total,
        shippingCharge,
        vat,
        status,
    )
}

data class Order(
    val orderId: String,
    val quantity: Int,
    val subTotal: Float,
    val total: Float,
    val shippingCharge: Float,
    val vat: Float?,
    val status: OrderTable.OrderStatus,
)