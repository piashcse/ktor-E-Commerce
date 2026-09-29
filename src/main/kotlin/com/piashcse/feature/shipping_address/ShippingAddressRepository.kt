package com.piashcse.feature.shipping_address

import com.piashcse.constants.AppConstants
import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.model.response.ShippingAddressResponse
import com.piashcse.utils.common.PaginatedResponse

interface ShippingAddressRepository {
    suspend fun createShippingAddress(
        userId: String,
        request: ShippingAddressRequest,
    ): ShippingAddressResponse

    suspend fun getShippingAddresses(
        userId: String,
        limit: Int = AppConstants.Pagination.DEFAULT_LIMIT,
        offset: Int = AppConstants.Pagination.DEFAULT_OFFSET,
    ): PaginatedResponse<ShippingAddressResponse>

    suspend fun updateShippingAddress(
        userId: String,
        addressId: String,
        request: ShippingAddressRequest,
    ): ShippingAddressResponse

    suspend fun deleteShippingAddress(
        userId: String,
        addressId: String,
    ): Boolean
}
