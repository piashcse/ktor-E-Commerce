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
    val shippingAddress = varchar("ship_address", 150)
    val shippingCity = varchar("ship_city", 50)
    val shippingPhone = integer("ship_phone")
    val shippingName = varchar("ship_name", 50).nullable()
    val shippingEmail = varchar("ship_email", 50).nullable()
    val shippingCountry = varchar("ship_country", 50).nullable()
}

class ShippingEntity(id: EntityID<String>) : BaseIntEntity(id, ShippingTable) {
    companion object : BaseIntEntityClass<ShippingEntity>(ShippingTable)

    var userId by ShippingTable.userId
    var orderId by ShippingTable.orderId
    var shippingAddress by ShippingTable.shippingAddress
    var shippingCity by ShippingTable.shippingCity
    var shippingPhone by ShippingTable.shippingPhone
    var shippingName by ShippingTable.shippingName
    var shippingEmail by ShippingTable.shippingEmail
    var shippingCountry by ShippingTable.shippingCountry


    fun response() =
        Shipping(userId.value, orderId.value, shippingAddress, shippingCity, shippingPhone, shippingName, shippingEmail, shippingCountry)
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
