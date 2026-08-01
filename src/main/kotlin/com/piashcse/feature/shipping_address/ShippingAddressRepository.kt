package com.piashcse.feature.shipping_address

import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.model.response.ShippingAddressResponse

/**
 * Facts about a shipping address used by the service layer to enforce authorization.
 */
data class ShippingAddressAccess(
    val addressId: String,
    val isOwner: Boolean,
)

/**
 * Persistence boundary for the Shipping Address aggregate.
 *
 * The repository is limited to data access (reads/writes + projection to DTOs).
 * All authorization and transaction orchestration live in the checkout service.
 */
interface ShippingAddressRepository {
    /**
     * Persists a new shipping address, clearing any other default address for
     * the user when the new one is default. Assumes the caller has authorized
     * the user.
     */
    suspend fun createShippingAddress(
        userId: String,
        request: ShippingAddressRequest,
    ): ShippingAddressResponse

    /**
     * Lists the user's shipping addresses.
     */
    suspend fun getShippingAddresses(userId: String): List<ShippingAddressResponse>

    /**
     * Resolves the authorization facts for a shipping address relative to
     * [userId]. Throws if the address does not exist.
     */
    suspend fun getShippingAddressAccess(
        userId: String,
        addressId: String,
    ): ShippingAddressAccess

    /**
     * Updates a shipping address, clearing any other default address for the
     * owner when the new one is default. Assumes the caller has authorized the
     * address.
     */
    suspend fun updateShippingAddress(
        addressId: String,
        request: ShippingAddressRequest,
    ): ShippingAddressResponse

    /**
     * Deletes a shipping address. Assumes the caller has authorized the address.
     */
    suspend fun deleteShippingAddress(addressId: String): Boolean
}
