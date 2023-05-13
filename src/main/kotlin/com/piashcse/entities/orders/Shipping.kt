package com.piashcse.entities.orders

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ShippingAddressTable : BaseIntIdTable("shipping_address") {
    val orderId = reference("order_id", OrdersTable.id)
    val shippingName = varchar("shipping_name", 50)
    val shippingPhone = integer("shipping_phone")
    val shippingEmail = varchar("shipping_email", 50)
    val shippingAddress1 = text("shipping_address_1")
    val shippingAddress2 = float("shipping_address_2")
    val shippingCity = float("shipping_city")
    val shippingCountry = float("shipping_country")
}

class ShippingEntity(id: EntityID<String>) : BaseIntEntity(id, ShippingAddressTable) {
    companion object : BaseIntEntityClass<ShippingEntity>(ShippingAddressTable)

    var orderId by ShippingAddressTable.orderId
    var shippingName by ShippingAddressTable.shippingName
    var shippingPhone by ShippingAddressTable.shippingPhone
    var shippingEmail by ShippingAddressTable.shippingEmail
    var shippingAddress1 by ShippingAddressTable.shippingAddress1
    var shippingAddress2 by ShippingAddressTable.shippingAddress2
    var shippingCity by ShippingAddressTable.shippingCity
    var shippingCountry by ShippingAddressTable.shippingCountry
}