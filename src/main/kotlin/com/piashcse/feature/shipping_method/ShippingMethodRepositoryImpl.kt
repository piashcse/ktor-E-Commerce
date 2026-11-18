package com.piashcse.feature.shipping_method

import com.piashcse.database.entities.ShippingMethodDAO
import com.piashcse.database.entities.ShippingMethodTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.mapper.toShippingMethodResponse
import com.piashcse.model.request.ShippingMethodRequest
import com.piashcse.model.response.ShippingMethodResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ForbiddenException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
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
        shopId: String?,
    ): PaginatedResponse<ShippingMethodResponse> =
        query {
            ShippingMethodTable.selectAll()
                .also { query ->
                    shopId?.takeIf { it.isNotBlank() }?.let { value ->
                        query.andWhere {
                            (ShippingMethodTable.shopId.isNull()) or
                                (ShippingMethodTable.shopId eq value.entityID(ShopTable))
                        }
                    }
                }
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

    override suspend fun getSellerShippingMethods(
        shopId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ShippingMethodResponse> =
        query {
            ShippingMethodTable.selectAll()
                .andWhere { ShippingMethodTable.shopId eq shopId.entityID(ShopTable) }
                .orderBy(ShippingMethodTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { ShippingMethodDAO.wrapRow(it).toShippingMethodResponse() }
        }

    override suspend fun createSellerShippingMethod(
        shopId: String,
        request: ShippingMethodRequest,
    ): ShippingMethodResponse =
        query {
            ShippingMethodDAO.new {
                name = request.name
                type = request.type
                price = request.price
                deliveryTime = request.deliveryTime
                this.shopId = shopId.entityID(ShopTable)
            }.toShippingMethodResponse()
        }

    override suspend fun updateSellerShippingMethod(
        shopId: String,
        methodId: String,
        request: ShippingMethodRequest,
    ): ShippingMethodResponse =
        query {
            val method = ShippingMethodDAO.findById(methodId) ?: methodId.throwNotFound("ShippingMethod")
            if (method.shopId?.value != shopId) throw ForbiddenException("Shipping method belongs to another shop")
            method.apply {
                name = request.name
                type = request.type
                price = request.price
                deliveryTime = request.deliveryTime
            }.toShippingMethodResponse()
        }

    override suspend fun deleteSellerShippingMethod(
        shopId: String,
        methodId: String,
    ): Boolean =
        query {
            val method = ShippingMethodDAO.findById(methodId) ?: methodId.throwNotFound("ShippingMethod")
            if (method.shopId?.value != shopId) throw ForbiddenException("Shipping method belongs to another shop")
            method.delete()
            true
        }
}
