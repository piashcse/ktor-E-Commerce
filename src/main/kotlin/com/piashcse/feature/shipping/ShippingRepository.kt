package com.piashcse.feature.shipping

import com.piashcse.model.request.ShippingRequest
import com.piashcse.model.request.UpdateShippingRequest
import com.piashcse.model.response.Shipping

interface ShippingRepository {
    suspend fun createShipping(userId: String, shippingRequest: ShippingRequest): Shipping
    suspend fun getShipping(userId: String, orderId: String): Shipping
    suspend fun updateShipping(userId: String, updateShipping: UpdateShippingRequest): Shipping
    suspend fun deleteShipping(userId: String, id: String):String
}