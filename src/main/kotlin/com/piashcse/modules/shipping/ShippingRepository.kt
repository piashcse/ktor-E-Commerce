package com.piashcse.modules.shipping

import com.piashcse.database.entities.Shipping
import com.piashcse.database.models.shipping.ShippingRequest
import com.piashcse.database.models.shipping.UpdateShipping

interface ShippingRepository {
    suspend fun createShipping(userId: String, shippingRequest: ShippingRequest): Shipping
    suspend fun getShipping(userId: String, orderId: String): Shipping
    suspend fun updateShipping(userId: String, updateShipping: UpdateShipping): Shipping
    suspend fun deleteShipping(userId: String, id: String):String
}