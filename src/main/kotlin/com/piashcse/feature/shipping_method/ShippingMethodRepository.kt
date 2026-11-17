package com.piashcse.feature.shipping_method

import com.piashcse.model.request.ShippingMethodRequest
import com.piashcse.model.response.ShippingMethodResponse
import com.piashcse.utils.common.PaginatedResponse

interface ShippingMethodRepository {
    suspend fun createShippingMethod(request: ShippingMethodRequest): ShippingMethodResponse

    suspend fun getShippingMethods(
        limit: Int = 20,
        offset: Int = 0,
        shopId: String? = null,
    ): PaginatedResponse<ShippingMethodResponse>

    suspend fun updateShippingMethod(
        methodId: String,
        request: ShippingMethodRequest,
    ): ShippingMethodResponse

    suspend fun deleteShippingMethod(methodId: String): Boolean

    suspend fun getSellerShippingMethods(
        shopId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ShippingMethodResponse>

    suspend fun createSellerShippingMethod(
        shopId: String,
        request: ShippingMethodRequest,
    ): ShippingMethodResponse

    suspend fun updateSellerShippingMethod(
        shopId: String,
        methodId: String,
        request: ShippingMethodRequest,
    ): ShippingMethodResponse

    suspend fun deleteSellerShippingMethod(
        shopId: String,
        methodId: String,
    ): Boolean
}
