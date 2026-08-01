package com.piashcse.feature.checkout

import com.piashcse.feature.order.OrderRepository
import com.piashcse.feature.shipping_address.ShippingAddressRepository
import com.piashcse.feature.shipping_method.ShippingMethodRepository
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.model.response.ShippingAddressResponse
import com.piashcse.model.response.ShippingMethodResponse

class CheckoutService(
    private val shippingAddressRepo: ShippingAddressRepository,
    private val shippingMethodRepo: ShippingMethodRepository,
    private val orderRepo: OrderRepository,
) {
    suspend fun createShippingAddress(
        userId: String,
        request: ShippingAddressRequest,
    ): ShippingAddressResponse = shippingAddressRepo.createShippingAddress(userId, request)

    suspend fun updateShippingAddress(
        userId: String,
        addressId: String,
        request: ShippingAddressRequest,
    ): ShippingAddressResponse = shippingAddressRepo.updateShippingAddress(userId, addressId, request)

    suspend fun deleteShippingAddress(
        userId: String,
        addressId: String,
    ): Boolean = shippingAddressRepo.deleteShippingAddress(userId, addressId)

    suspend fun getShippingAddresses(userId: String): List<ShippingAddressResponse> =
        shippingAddressRepo.getShippingAddresses(userId)

    suspend fun getShippingMethods(): List<ShippingMethodResponse> = shippingMethodRepo.getShippingMethods()

    suspend fun getCheckoutSummary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutSummaryResponse = orderRepo.getCheckoutSummary(userId, checkoutRequest)

    suspend fun placeOrder(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): List<OrderResponse> = orderRepo.placeOrder(userId, checkoutRequest)
}
