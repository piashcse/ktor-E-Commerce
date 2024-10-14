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
    val cancelOrder = bool("cancel_order").clientDefault { false }
    val coupon = varchar("coupon", 50).nullable()
    val status = varchar("status", 30).clientDefault { "pending" }
    val statusCode = integer("status_code").clientDefault { 0 }
}

class OrderEntity(id: EntityID<String>) : BaseIntEntity(id, OrderTable) {
    companion object : BaseIntEntityClass<OrderEntity>(OrderTable)

    var userId by OrderTable.userId
    var quantity by OrderTable.quantity
    var subTotal by OrderTable.subTotal
    var total by OrderTable.total
    var shippingCharge by OrderTable.shippingCharge
    var vat by OrderTable.vat
    var cancelOrder by OrderTable.cancelOrder
    var coupon by OrderTable.coupon
    var status by OrderTable.status
    var statusCode by OrderTable.statusCode
    fun response() = Order(
        id.value,
        quantity,
        subTotal,
        total,
        shippingCharge,
        vat,
        cancelOrder,
        coupon,
        status,
        statusCode
    )
}
data class Order(
    val orderId: String,
    val quantity: Int,
    val subTotal: Float,
    val total: Float,
    val shippingCharge: Float,
    val vat: Float?,
    val cancelOrder: Boolean,
    val coupon: String?,
    val status: String,
    val statusCode: Int
)