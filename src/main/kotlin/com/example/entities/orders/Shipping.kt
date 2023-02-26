package com.example.entities.orders

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ShippingTable : BaseIntIdTable("shipping") {
    val orderId = reference("order_id", OrdersTable.id)
    val shippingName = varchar("shipping_name", 50)
    val shippingPhone = integer("shipping_phone")
    val shippingEmail = varchar("shipping_email", 50)
    val shippingAddress1 = text("shipping_address_1")
    val shippingAddress2 = float("shipping_address_2")
    val shippingCity = float("shipping_city")
    val shippingCountry = float("shipping_country")
}

class ShippingEntity(id: EntityID<String>) : BaseIntEntity(id, ShippingTable) {
    companion object : BaseIntEntityClass<ShippingEntity>(ShippingTable)

    var orderId by ShippingTable.orderId
    var shippingName by ShippingTable.shippingName
    var shippingPhone by ShippingTable.shippingPhone
    var shippingEmail by ShippingTable.shippingEmail
    var shippingAddress1 by ShippingTable.shippingAddress1
    var shippingAddress2 by ShippingTable.shippingAddress2
    var shippingCity by ShippingTable.shippingCity
    var shippingCountry by ShippingTable.shippingCountry
}