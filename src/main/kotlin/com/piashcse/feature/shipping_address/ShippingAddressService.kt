package com.piashcse.feature.shipping_address

import com.piashcse.database.entities.ShippingAddressDAO
import com.piashcse.database.entities.ShippingAddressTable
import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.model.response.ShippingAddressResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq

class ShippingAddressService : ShippingAddressRepository {
    override suspend fun createShippingAddress(userId: String, request: ShippingAddressRequest): ShippingAddressResponse = query {
        if (request.isDefault) {
            // Unset other default addresses for this user
            ShippingAddressDAO.find { 
                ShippingAddressTable.userId eq userId and (ShippingAddressTable.isDefault eq true)
            }.forEach { it.isDefault = false }
        }

        ShippingAddressDAO.new {
            this.userId = EntityID(userId, ShippingAddressTable)
            firstName = request.firstName
            lastName = request.lastName
            email = request.email
            phoneNumber = request.phoneNumber
            streetAddress = request.streetAddress
            city = request.city
            state = request.state
            country = request.country
            zipCode = request.zipCode
            isDefault = request.isDefault
        }.response()
    }

    override suspend fun getShippingAddresses(userId: String): List<ShippingAddressResponse> = query {
        ShippingAddressDAO.find { ShippingAddressTable.userId eq userId }.map { it.response() }
    }

    override suspend fun updateShippingAddress(userId: String, addressId: String, request: ShippingAddressRequest): ShippingAddressResponse = query {
        val address = ShippingAddressDAO.find { 
            ShippingAddressTable.userId eq userId and (ShippingAddressTable.id eq addressId)
        }.singleOrNull() ?: addressId.throwNotFound("ShippingAddress")

        if (request.isDefault && !address.isDefault) {
            // Unset other default addresses for this user
            ShippingAddressDAO.find { 
                ShippingAddressTable.userId eq userId and (ShippingAddressTable.isDefault eq true)
            }.forEach { it.isDefault = false }
        }

        address.apply {
            firstName = request.firstName
            lastName = request.lastName
            email = request.email
            phoneNumber = request.phoneNumber
            streetAddress = request.streetAddress
            city = request.city
            state = request.state
            country = request.country
            zipCode = request.zipCode
            isDefault = request.isDefault
        }.response()
    }

    override suspend fun deleteShippingAddress(userId: String, addressId: String): Boolean = query {
        val address = ShippingAddressDAO.find { 
            ShippingAddressTable.userId eq userId and (ShippingAddressTable.id eq addressId)
        }.singleOrNull() ?: addressId.throwNotFound("ShippingAddress")

        address.delete()
        true
    }
}
