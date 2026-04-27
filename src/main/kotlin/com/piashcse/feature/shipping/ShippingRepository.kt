package com.piashcse.feature.shipping

import com.piashcse.model.request.ShippingRequest
import com.piashcse.model.request.UpdateShippingRequest
import com.piashcse.model.response.ShippingResponse

interface ShippingRepository {
    suspend fun createShipping(userId: String, shippingRequest: ShippingRequest): ShippingResponse
    suspend fun getShipping(userId: String, orderId: String): ShippingResponse
    suspend fun updateShipping(userId: String, updateShipping: UpdateShippingRequest): ShippingResponse
    suspend fun deleteShipping(userId: String, id: String):String
}