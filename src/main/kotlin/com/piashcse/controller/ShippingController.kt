package com.piashcse.controller

import com.piashcse.entities.Shipping
import com.piashcse.entities.ShippingDAO
import com.piashcse.entities.ShippingTable
import com.piashcse.models.shipping.ShippingRequest
import com.piashcse.models.shipping.UpdateShipping
import com.piashcse.repository.ShippingRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

/**
 * Controller for managing shipping details. Provides methods to add, get, update, and delete shipping information.
 */
class ShippingController : ShippingRepo {

    /**
     * Adds new shipping details for an order. If the shipping details for the same order already exist, an exception is thrown.
     *
     * @param userId The ID of the user for whom the shipping details are being added.
     * @param addShipping The shipping details to be added.
     * @return The added shipping details.
     * @throws alreadyExistException If the shipping details for the specified order already exist.
     */
    override suspend fun addShipping(userId: String, addShipping: ShippingRequest): Shipping = query {
        val isShippingExist = ShippingDAO.find {
            ShippingTable.userId eq userId and (ShippingTable.orderId eq addShipping.orderId)
        }.toList().singleOrNull()
        isShippingExist?.let {
            throw addShipping.orderId.alreadyExistException()
        } ?: ShippingDAO.new {
            this.userId = EntityID(userId, ShippingTable)
            this.orderId = EntityID(addShipping.orderId, ShippingTable)
            shippingAddress = addShipping.shipAddress
            shippingCity = addShipping.shipCity
            shippingPhone = addShipping.shipPhone
            shippingName = addShipping.shipName
            shippingEmail = addShipping.shipEmail
            shippingCountry = addShipping.shipCountry
        }.response()
    }

    /**
     * Retrieves shipping details for a specific order and user.
     *
     * @param userId The ID of the user whose shipping details are to be retrieved.
     * @param orderId The ID of the order for which shipping details are requested.
     * @return The shipping details for the specified order.
     * @throws orderId.notFoundException If the shipping details for the specified order ID are not found.
     */
    override suspend fun getShipping(userId: String, orderId: String): Shipping = query {
        val isShippingExist = ShippingDAO.find {
            ShippingTable.userId eq userId and (ShippingTable.orderId eq orderId)
        }.toList().singleOrNull()
        isShippingExist?.response() ?: throw orderId.notFoundException()
    }

    /**
     * Updates existing shipping details. If the shipping record does not exist, an exception is thrown.
     *
     * @param userId The ID of the user whose shipping details are being updated.
     * @param updateShipping The updated shipping details.
     * @return The updated shipping details.
     * @throws alreadyExistException If the shipping ID does not exist for the specified user.
     */
    override suspend fun updateShipping(userId: String, updateShipping: UpdateShipping): Shipping = query {
        val isShippingExist = ShippingDAO.find {
            ShippingTable.userId eq userId and (ShippingTable.id eq updateShipping.id)
        }.toList().singleOrNull()

        isShippingExist?.let {
            it.shippingAddress = updateShipping.shipAddress ?: it.shippingAddress
            it.shippingCity = updateShipping.shipCity ?: it.shippingCity
            it.shippingPhone = updateShipping.shipPhone ?: it.shippingPhone
            it.shippingName = updateShipping.shipName ?: it.shippingName
            it.shippingEmail = updateShipping.shipEmail ?: it.shippingEmail
            it.shippingCountry = updateShipping.shipCountry ?: it.shippingCountry
            it.response()
        } ?: throw updateShipping.id.alreadyExistException()
    }

    /**
     * Deletes shipping details for a specific user and shipping ID.
     *
     * @param userId The ID of the user whose shipping details are to be deleted.
     * @param id The ID of the shipping record to be deleted.
     * @return The ID of the deleted shipping record.
     * @throws id.notFoundException If the shipping record with the specified ID does not exist.
     */
    override suspend fun deleteShipping(userId: String, id: String): String = query {
        val isShippingExist = ShippingDAO.find {
            ShippingTable.userId eq userId and (ShippingTable.id eq id)
        }.toList().singleOrNull()
        isShippingExist?.let {
            it.delete()
            id
        } ?: throw id.notFoundException()
    }
}