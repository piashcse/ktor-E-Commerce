package com.piashcse.controller

import com.piashcse.entities.Shipping
import com.piashcse.entities.ShippingEntity
import com.piashcse.entities.ShippingTable
import com.piashcse.models.shipping.AddShipping
import com.piashcse.models.shipping.UpdateShipping
import com.piashcse.repository.ShippingRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class ShippingController : ShippingRepo {
    override suspend fun addShipping(userId: String, addShipping: AddShipping): Shipping = query {
        val isShippingExist = ShippingEntity.find {
            ShippingTable.userId eq userId and (ShippingTable.orderId eq addShipping.orderId)
        }.toList().singleOrNull()
        isShippingExist?.let {
            ShippingEntity.new {
                this.userId = EntityID(userId, ShippingTable)
                this.orderId = EntityID(addShipping.orderId, ShippingTable)
                shippingAddress = addShipping.shipAddress
                shippingCity = addShipping.shipCity
                shippingPhone = addShipping.shipPhone
                shippingName = addShipping.shipName
                shippingEmail = addShipping.shipEmail
                shippingCountry = addShipping.shipCountry

            }.response()
        } ?: throw addShipping.orderId.alreadyExistException()
    }

    override suspend fun getShipping(userId: String, orderId: String): Shipping = query {
        val isShippingExist = ShippingEntity.find {
            ShippingTable.userId eq userId and (ShippingTable.orderId eq orderId)
        }.toList().singleOrNull()
        isShippingExist?.response() ?: throw orderId.notFoundException()
    }

    override suspend fun updateShipping(userId: String, updateShipping: UpdateShipping): Shipping = query {
        val isShippingExist = ShippingEntity.find {
            ShippingTable.userId eq userId and (ShippingTable.orderId eq updateShipping.orderId)
        }.toList().singleOrNull()

        isShippingExist?.let {
            it.shippingAddress = updateShipping.shipAddress ?: it.shippingAddress
            it.shippingCity = updateShipping.shipCity ?: it.shippingCity
            it.shippingPhone = updateShipping.shipPhone ?: it.shippingPhone
            it.shippingName = updateShipping.shipName ?: it.shippingName
            it.shippingEmail = updateShipping.shipEmail ?: it.shippingEmail
            it.shippingCountry = updateShipping.shipCountry ?: it.shippingCountry
            it.response()
        } ?: throw updateShipping.orderId.alreadyExistException()
    }

    override suspend fun deleteShipping(userId: String, orderId: String): String = query {
        val isShippingExist = ShippingEntity.find {
            ShippingTable.userId eq userId and (ShippingTable.orderId eq orderId)
        }.toList().singleOrNull()
        isShippingExist?.let {
            it.delete()
            userId
        } ?: throw orderId.notFoundException()
    }
}