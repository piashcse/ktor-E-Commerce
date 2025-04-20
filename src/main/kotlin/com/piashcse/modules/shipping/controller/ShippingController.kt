package com.piashcse.modules.shipping.controller

import com.piashcse.database.entities.Shipping
import com.piashcse.database.entities.ShippingDAO
import com.piashcse.database.entities.ShippingTable
import com.piashcse.database.models.shipping.ShippingRequest
import com.piashcse.database.models.shipping.UpdateShipping
import com.piashcse.modules.shipping.repository.ShippingRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID

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
    override suspend fun createShipping(userId: String, shippingRequest: ShippingRequest): Shipping = query {
        val isShippingExist = ShippingDAO.Companion.find {
            ShippingTable.orderId eq shippingRequest.orderId
        }.toList().singleOrNull()
        isShippingExist?.let {
            throw shippingRequest.orderId.alreadyExistException()
        } ?: ShippingDAO.Companion.new {
            this.orderId = EntityID(shippingRequest.orderId, ShippingTable)
            address = shippingRequest.address
            city = shippingRequest.city
            country = shippingRequest.country
            phone = shippingRequest.phone
            email = shippingRequest.email
            shippingMethod = shippingRequest.shippingMethod
            trackingNumber = "TRK-${System.currentTimeMillis()}-${(1000..9999).random()}"
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
        val isShippingExist = ShippingDAO.Companion.find {
            ShippingTable.orderId eq orderId
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
        val isShippingExist = ShippingDAO.Companion.find {
            ShippingTable.id eq updateShipping.id
        }.toList().singleOrNull()

        isShippingExist?.let {
            it.address = updateShipping.address ?: it.address
            it.city = updateShipping.city ?: it.city
            it.country = updateShipping.country ?: it.country
            it.phone = updateShipping.phone ?: it.phone
            it.email = updateShipping.email ?: it.email
            it.shippingMethod = updateShipping.shippingMethod ?: it.shippingMethod
            it.status = updateShipping.status ?: it.status
            it.trackingNumber = updateShipping.trackingNumber ?: it.trackingNumber
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
        val isShippingExist = ShippingDAO.Companion.find {
            ShippingTable.id eq id
        }.toList().singleOrNull()
        isShippingExist?.let {
            it.delete()
            id
        } ?: throw id.notFoundException()
    }
}