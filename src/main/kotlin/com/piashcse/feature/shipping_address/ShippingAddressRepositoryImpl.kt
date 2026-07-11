package com.piashcse.feature.shipping_address

import com.piashcse.database.entities.ShippingAddressDAO
import com.piashcse.database.entities.ShippingAddressTable
import com.piashcse.mapper.toShippingAddressResponse
import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.model.response.ShippingAddressResponse
import com.piashcse.utils.extension.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

class ShippingAddressRepositoryImpl : ShippingAddressRepository {
    override suspend fun createShippingAddress(
        userId: String,
        request: ShippingAddressRequest,
    ): ShippingAddressResponse =
        query {
            if (request.isDefault) {
                ShippingAddressDAO.find {
                    ShippingAddressTable.userId eq userId and (ShippingAddressTable.isDefault eq true)
                }.forEach { it.isDefault = false }
            }

            ShippingAddressDAO.new {
                this.userId = userId.entityID(ShippingAddressTable)
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
            }.toShippingAddressResponse()
        }

    override suspend fun getShippingAddresses(userId: String): List<ShippingAddressResponse> =
        query {
            ShippingAddressDAO.find { ShippingAddressTable.userId eq userId }.map { it.toShippingAddressResponse() }
        }

    override suspend fun updateShippingAddress(
        userId: String,
        addressId: String,
        request: ShippingAddressRequest,
    ): ShippingAddressResponse =
        query {
            val address =
                ShippingAddressDAO.findById(addressId) ?: addressId.throwNotFound("ShippingAddress")

            address.verifyOwnership(userId, "shipping address") { it.userId.value }

            if (request.isDefault && !address.isDefault) {
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
            }.toShippingAddressResponse()
        }

    override suspend fun deleteShippingAddress(
        userId: String,
        addressId: String,
    ): Boolean =
        query {
            val address =
                ShippingAddressDAO.findById(addressId) ?: addressId.throwNotFound("ShippingAddress")

            address.verifyOwnership(userId, "shipping address") { it.userId.value }

            address.delete()
            true
        }
}
