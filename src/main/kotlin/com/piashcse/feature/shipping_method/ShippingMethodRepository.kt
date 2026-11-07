package com.piashcse.feature.shipping_method

import com.piashcse.model.request.ShippingMethodRequest
import com.piashcse.model.response.ShippingMethodResponse
import com.piashcse.utils.common.PaginatedResponse

interface ShippingMethodRepository {
    suspend fun createShippingMethod(request: ShippingMethodRequest): ShippingMethodResponse

    suspend fun getShippingMethods(
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<ShippingMethodResponse>

    suspend fun updateShippingMethod(
        methodId: String,
        request: ShippingMethodRequest,
    ): ShippingMethodResponse

    suspend fun deleteShippingMethod(methodId: String): Boolean
}
