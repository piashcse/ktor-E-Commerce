package com.piashcse.feature.shipping_method

import com.piashcse.model.request.ShippingMethodRequest
import com.piashcse.model.response.ShippingMethodResponse

class ShippingMethodService(private val shippingMethodRepo: ShippingMethodRepository) {
    suspend fun createShippingMethod(request: ShippingMethodRequest): ShippingMethodResponse =
        shippingMethodRepo.createShippingMethod(request)

    suspend fun getShippingMethods(): List<ShippingMethodResponse> = shippingMethodRepo.getShippingMethods()

    suspend fun updateShippingMethod(
        methodId: String,
        request: ShippingMethodRequest,
    ): ShippingMethodResponse = shippingMethodRepo.updateShippingMethod(methodId, request)

    suspend fun deleteShippingMethod(methodId: String): Boolean = shippingMethodRepo.deleteShippingMethod(methodId)
}
