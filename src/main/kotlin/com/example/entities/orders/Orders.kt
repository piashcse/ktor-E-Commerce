package com.example.entities.orders

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import com.example.entities.user.UserTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object OrdersTable : BaseIntIdTable("orders") {
    val userId = reference("user_id", UserTable.id)
    val paymentId = varchar("payment_id", 50)
    val paymentType = varchar("payment_type", 50)
    val quantity = integer("quantity")
    val subTotal = float("sub_total")
    val total = float("total")
    val shippingCharge = float("shopping_charge")
    val vat = float("vat")
    val cancelOrder = integer("cancel_order")
    val coupon = varchar("coupon", 50).nullable()
    val status = integer("status")
    val statusCode = integer("status_code")
    val date = date("date")
}

class OrderEntity(id: EntityID<String>) : BaseIntEntity(id, OrdersTable) {
    companion object : BaseIntEntityClass<OrderEntity>(OrdersTable)

    var userId by OrdersTable.userId
    var paymentId by OrdersTable.paymentId
    var paymentType by OrdersTable.paymentType
    var quantity by OrdersTable.quantity
    var subTotal by OrdersTable.subTotal
    var total by OrdersTable.total
    var shippingCharge by OrdersTable.shippingCharge
    var vat by OrdersTable.vat
    var cancelOrder by OrdersTable.cancelOrder
    var coupon by OrdersTable.coupon
    var status by OrdersTable.status
    var statusCode by OrdersTable.statusCode
    // fun shopResponse() = Shop(id.value, shop_name)
}