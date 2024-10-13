package com.piashcse.repository

import com.piashcse.entities.Shipping
import com.piashcse.models.shipping.AddShipping
import com.piashcse.models.shipping.UpdateShipping

interface ShippingRepo {
    suspend fun addShipping(userId: String, addShipping: AddShipping): Shipping
    suspend fun getShipping(userId: String, orderId: String):Shipping
    suspend fun updateShipping(userId: String, updateShipping: UpdateShipping): Shipping
    suspend fun deleteShipping(userId: String, id: String):String
}