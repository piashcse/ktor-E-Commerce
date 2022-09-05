package com.example.entities.orders

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import com.example.entities.user.UserTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object OrdersTable : BaseIntIdTable("orders") {
    val userId = reference("user_id", UserTable.id)
    val orderAmount = float("order_amount")
    val shippingAddress1 = text("shipping_address_1")
    val shippingAddress2 = text("shipping_address_2")
    val city = text("city")
    val orderEmail = text("order_email")
    val orderDate = datetime("order_date")
    val orderTrackingNumber = text("order_tracking_number")
}

class OrdersEntity(id: EntityID<String>) : BaseIntEntity(id, OrdersTable) {
    companion object : BaseIntEntityClass<OrdersEntity>(OrdersTable)

    var userId by OrdersTable.userId
    var orderAmount by OrdersTable.orderAmount
    var shippingAddress1 by OrdersTable.shippingAddress1
    var shippingAddress2 by OrdersTable.shippingAddress2
    var city by OrdersTable.city
    var orderEmail by OrdersTable.orderEmail
    var orderDate by OrdersTable.orderDate
    var orderTrackingNumber by OrdersTable.orderTrackingNumber
    // fun shopResponse() = Shop(id.value, shop_name)
}