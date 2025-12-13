package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.Shipping
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ShippingTable : BaseIdTable("shipping") {
    val orderId = reference("order_id", OrderTable.id)
    val address = varchar("address", 150)
    val city = varchar("city", 50)
    val country = varchar("ship_country", 50).nullable()
    val phone = varchar("phone", 20)
    val email = varchar("ship_email", 50).nullable()
    val shippingMethod = varchar("shipping_method", 50).nullable()
    val status = enumerationByName("status", 20, ShippingStatus::class).clientDefault { ShippingStatus.PENDING }
    val trackingNumber = varchar("tracking_number", 255).nullable()

    enum class ShippingStatus {
        PENDING, SHIPPED, DELIVERED, CANCELLED;
        val isActive get() = this in listOf(PENDING, SHIPPED)
        val isCompleted get() = this == DELIVERED
        val isCancelable get() = this == PENDING
    }
}

class ShippingDAO(id: EntityID<String>) : BaseEntity(id, ShippingTable) {
    companion object : BaseEntityClass<ShippingDAO>(ShippingTable, ShippingDAO::class.java)

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