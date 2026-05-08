package com.piashcse.feature.shipping_method

import com.piashcse.model.request.ShippingMethodRequest
import com.piashcse.model.response.ShippingMethodResponse

interface ShippingMethodRepository {
    suspend fun createShippingMethod(request: ShippingMethodRequest): ShippingMethodResponse

    suspend fun getShippingMethods(): List<ShippingMethodResponse>

    suspend fun updateShippingMethod(
        methodId: String,
        request: ShippingMethodRequest,
    ): ShippingMethodResponse

    suspend fun deleteShippingMethod(methodId: String): Boolean
}
