package com.piashcse.feature.shipping_address

import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.model.response.ShippingAddressResponse

interface ShippingAddressRepository {
    suspend fun createShippingAddress(userId: String, request: ShippingAddressRequest): ShippingAddressResponse
    suspend fun getShippingAddresses(userId: String): List<ShippingAddressResponse>
    suspend fun updateShippingAddress(userId: String, addressId: String, request: ShippingAddressRequest): ShippingAddressResponse
    suspend fun deleteShippingAddress(userId: String, addressId: String): Boolean
}
