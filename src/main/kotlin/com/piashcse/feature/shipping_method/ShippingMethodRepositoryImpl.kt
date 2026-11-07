package com.piashcse.feature.shipping_method

import com.piashcse.database.entities.ShippingMethodDAO
import com.piashcse.database.entities.ShippingMethodTable
import com.piashcse.mapper.toShippingMethodResponse
import com.piashcse.model.request.ShippingMethodRequest
import com.piashcse.model.response.ShippingMethodResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.selectAll

class ShippingMethodRepositoryImpl : ShippingMethodRepository {
    override suspend fun createShippingMethod(request: ShippingMethodRequest): ShippingMethodResponse =
        query {
            ShippingMethodDAO.new {
                name = request.name
                type = request.type
                price = request.price
                deliveryTime = request.deliveryTime
            }.toShippingMethodResponse()
        }

    override suspend fun getShippingMethods(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ShippingMethodResponse> =
        query {
            ShippingMethodTable.selectAll()
                .orderBy(ShippingMethodTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { ShippingMethodDAO.wrapRow(it).toShippingMethodResponse() }
        }

    override suspend fun updateShippingMethod(
        methodId: String,
        request: ShippingMethodRequest,
    ): ShippingMethodResponse =
        query {
            val method = ShippingMethodDAO.findById(methodId) ?: methodId.throwNotFound("ShippingMethod")
            method.apply {
                name = request.name
                type = request.type
                price = request.price
                deliveryTime = request.deliveryTime
            }.toShippingMethodResponse()
        }

    override suspend fun deleteShippingMethod(methodId: String): Boolean =
        query {
            val method = ShippingMethodDAO.findById(methodId) ?: methodId.throwNotFound("ShippingMethod")
            method.delete()
            true
        }
}
