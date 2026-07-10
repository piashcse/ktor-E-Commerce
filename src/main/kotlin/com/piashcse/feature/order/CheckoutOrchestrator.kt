package com.piashcse.feature.order

import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderResponse

class CheckoutOrchestrator(private val orderRepo: OrderRepository) {

    suspend fun placeOrder(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): List<OrderResponse> =
        orderRepo.placeOrder(userId, checkoutRequest)

    suspend fun getCheckoutSummary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutSummaryResponse =
        orderRepo.getCheckoutSummary(userId, checkoutRequest)
}
