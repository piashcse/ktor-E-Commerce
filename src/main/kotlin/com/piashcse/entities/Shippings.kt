package com.piashcse.entities

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import com.piashcse.entities.orders.OrdersTable
import com.piashcse.entities.user.UserTable
import org.jetbrains.exposed.dao.id.EntityID

object ShippingTable : BaseIntIdTable("Shipping") {
    val userId = reference("user_id", UserTable.id)
    val orderId = reference("order_id", OrdersTable.id)
    val shipAddress = varchar("ship_address", 150)
    val shipCity = varchar("ship_city", 50)
    val shipPhone = integer("ship_phone")
    val shipName = varchar("ship_name", 50).nullable()
    val shipEmail = varchar("ship_email", 50).nullable()
    val shipCountry = varchar("ship_country", 50).nullable()
}

class ShippingEntity(id: EntityID<String>) : BaseIntEntity(id, ShippingTable) {
    companion object : BaseIntEntityClass<ShippingEntity>(ShippingTable)

    var userId by ShippingTable.userId
    var orderId by ShippingTable.orderId
    var shipAddress by ShippingTable.shipAddress
    var shipCity by ShippingTable.shipCity
    var shipPhone by ShippingTable.shipPhone
    var shipName by ShippingTable.shipName
    var shipEmail by ShippingTable.shipEmail
    var shipCountry by ShippingTable.shipCountry

    fun response() =
        Shipping(userId.value, orderId.value, shipAddress, shipCity, shipPhone, shipName, shipEmail, shipCountry)
}

data class Shipping(
    var userId: String,
    var orderId: String,
    var shipAddress: String,
    var shipCity: String,
    var shipPhone: Int,
    var shipName: String?,
    var shipEmail: String?,
    var shipCountry: String?
)
