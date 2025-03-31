package com.piashcse.repository

import com.piashcse.entities.Shipping
import com.piashcse.models.shipping.ShippingRequest
import com.piashcse.models.shipping.UpdateShipping

interface ShippingRepo {
    suspend fun createShipping(userId: String, shippingRequest: ShippingRequest): Shipping
    suspend fun getShipping(userId: String, orderId: String):Shipping
    suspend fun updateShipping(userId: String, updateShipping: UpdateShipping): Shipping
    suspend fun deleteShipping(userId: String, id: String):String
}