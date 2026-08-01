package com.piashcse.feature.checkout

import com.piashcse.constants.Message
import com.piashcse.feature.order.OrderService
import com.piashcse.feature.shipping_address.ShippingAddressRepository
import com.piashcse.feature.shipping_method.ShippingMethodRepository
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.model.response.ShippingAddressResponse
import com.piashcse.model.response.ShippingMethodResponse
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.validator.ForbiddenException

class CheckoutService(
    private val shippingAddressRepo: ShippingAddressRepository,
    private val shippingMethodRepo: ShippingMethodRepository,
    private val orderService: OrderService,
) {
    /**
     * Creates a shipping address. Runs in a retryable transaction.
     */
    suspend fun createShippingAddress(
        userId: String,
        request: ShippingAddressRequest,
    ): ShippingAddressResponse = suspendRetryQuery {
        shippingAddressRepo.createShippingAddress(userId, request)
    }

    /**
     * Updates a shipping address after verifying ownership. Runs in a retryable
     * transaction.
     */
    suspend fun updateShippingAddress(
        userId: String,
        addressId: String,
        request: ShippingAddressRequest,
    ): ShippingAddressResponse = suspendRetryQuery {
        val access = shippingAddressRepo.getShippingAddressAccess(userId, addressId)
        if (!access.isOwner) throw ForbiddenException(Message.Errors.notOwner("shipping address"))
        shippingAddressRepo.updateShippingAddress(addressId, request)
    }

    /**
     * Deletes a shipping address after verifying ownership. Runs in a retryable
     * transaction.
     */
    suspend fun deleteShippingAddress(
        userId: String,
        addressId: String,
    ): Boolean = suspendRetryQuery {
        val access = shippingAddressRepo.getShippingAddressAccess(userId, addressId)
        if (!access.isOwner) throw ForbiddenException(Message.Errors.notOwner("shipping address"))
        shippingAddressRepo.deleteShippingAddress(addressId)
    }

    suspend fun getShippingAddresses(userId: String): List<ShippingAddressResponse> =
        shippingAddressRepo.getShippingAddresses(userId)

    suspend fun getShippingMethods(): List<ShippingMethodResponse> = shippingMethodRepo.getShippingMethods()

    suspend fun getCheckoutSummary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutSummaryResponse = orderService.getCheckoutSummary(userId, checkoutRequest)

    suspend fun placeOrder(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): List<OrderResponse> = orderService.placeOrder(userId, checkoutRequest)
}
