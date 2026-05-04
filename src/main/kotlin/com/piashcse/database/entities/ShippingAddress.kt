package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.ShippingAddressResponse
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ShippingAddressTable : BaseIdTable("shipping_address") {
    val userId = reference("user_id", UserTable.id)
    val firstName = varchar("first_name", 50)
    val lastName = varchar("last_name", 50)
    val email = varchar("email", 50)
    val phoneNumber = varchar("phone_number", 20)
    val streetAddress = varchar("street_address", 150)
    val city = varchar("city", 50)
    val state = varchar("state", 50).nullable()
    val country = varchar("country", 50)
    val zipCode = varchar("zip_code", 20)
    val isDefault = bool("is_default").default(false)
}

class ShippingAddressDAO(id: EntityID<String>) : BaseEntity(id, ShippingAddressTable) {
    companion object : BaseEntityClass<ShippingAddressDAO>(ShippingAddressTable, ShippingAddressDAO::class.java)

    var userId by ShippingAddressTable.userId
    var firstName by ShippingAddressTable.firstName
    var lastName by ShippingAddressTable.lastName
    var email by ShippingAddressTable.email
    var phoneNumber by ShippingAddressTable.phoneNumber
    var streetAddress by ShippingAddressTable.streetAddress
    var city by ShippingAddressTable.city
    var state by ShippingAddressTable.state
    var country by ShippingAddressTable.country
    var zipCode by ShippingAddressTable.zipCode
    var isDefault by ShippingAddressTable.isDefault

    fun response() = ShippingAddressResponse(
        id = id.value,
        userId = userId.value,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phoneNumber = phoneNumber,
        streetAddress = streetAddress,
        city = city,
        state = state,
        country = country,
        zipCode = zipCode,
        isDefault = isDefault
    )
}
