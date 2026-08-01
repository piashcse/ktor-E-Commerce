package com.piashcse.feature.shipping_method

import com.piashcse.database.entities.ShippingMethodDAO
import com.piashcse.database.entities.ShippingMethodTable
import com.piashcse.mapper.toShippingMethodResponse
import com.piashcse.model.request.ShippingMethodRequest
import com.piashcse.model.response.ShippingMethodResponse
import com.piashcse.repository.base.BaseCrudRepository

class ShippingMethodRepositoryImpl : ShippingMethodRepository,
    BaseCrudRepository<ShippingMethodDAO, ShippingMethodResponse>(
        ShippingMethodDAO,
        ShippingMethodTable,
        "ShippingMethod",
    ) {

    override fun ShippingMethodDAO.toResponse(): ShippingMethodResponse = toShippingMethodResponse()

    override suspend fun createShippingMethod(request: ShippingMethodRequest): ShippingMethodResponse =
        create {
            name = request.name
            type = request.type
            price = request.price
            deliveryTime = request.deliveryTime
        }

    override suspend fun getShippingMethods(): List<ShippingMethodResponse> = listAll()

    override suspend fun updateShippingMethod(
        methodId: String,
        request: ShippingMethodRequest,
    ): ShippingMethodResponse = update(methodId) {
        name = request.name
        type = request.type
        price = request.price
        deliveryTime = request.deliveryTime
    }

    override suspend fun deleteShippingMethod(methodId: String): Boolean {
        delete(methodId)
        return true
    }
}
