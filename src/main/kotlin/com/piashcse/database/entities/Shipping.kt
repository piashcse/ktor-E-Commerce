package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ShippingTable : BaseIntIdTable("shipping") {
    val orderId = reference("order_id", OrderTable.id)
    val address = varchar("address", 150)
    val city = varchar("city", 50)
    val country = varchar("ship_country", 50).nullable()
    val phone = integer("phone")
    val email = varchar("ship_email", 50).nullable()
    val shippingMethod = varchar("shipping_method", 50).nullable()
    val status = enumerationByName("status", 20, ShippingStatus::class).clientDefault { ShippingStatus.PENDING }
    val trackingNumber = varchar("tracking_number", 255).nullable()

    enum class ShippingStatus {
        PENDING, SHIPPED, DELIVERED, CANCELLED
    }
}

class ShippingDAO(id: EntityID<String>) : BaseIntEntity(id, ShippingTable) {
    companion object : BaseIntEntityClass<ShippingDAO>(ShippingTable)

    var orderId by ShippingTable.orderId
    var address by ShippingTable.address
    var city by ShippingTable.city
    var phone by ShippingTable.phone
    var country by ShippingTable.country
    var shippingMethod by ShippingTable.shippingMethod
    var email by ShippingTable.email
    var status by ShippingTable.status
    var trackingNumber by ShippingTable.trackingNumber

    fun response() =
        Shipping(
            id.value,
            orderId.value,
            status,
            address,
            city,
            country,
            phone,
            shippingMethod,
            email,
            trackingNumber,
        )
}

data class Shipping(
    val id: String,
    var orderId: String,
    var status: ShippingTable.ShippingStatus,
    var address: String,
    var city: String,
    var country: String?,
    var phone: Int,
    var email: String?,
    var shippingMethod: String?,
    var trackingNumber: String?,
)
