package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.OrderRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.PaginatedResponse
import java.time.Instant

/**
 * Facts about an order used by the service layer to enforce authorization.
 */
data class OrderAccess(
    val orderId: String,
    val currentStatus: OrderStatus,
    val isCustomer: Boolean,
    val isSeller: Boolean,
    val isAdmin: Boolean,
)

/**
 * Facts used to authorize order placement, resolving the shipping address so
 * the service can enforce ownership.
 */
data class PlaceOrderAccess(
    val isShippingAddressOwner: Boolean,
)

/**
 * Persistence boundary for the Order aggregate.
 *
 * The repository is limited to data access (reads/writes + projection to DTOs).
 * All authorization, domain rules, validation and event publishing live in
 * [OrderService].
 */
interface OrderRepository {
    /**
     * Places a new order from the user's cart. Assumes the caller has already
     * authorized the request (see [getPlaceOrderAccess]); applies stock
     * reservations, coupon consumption, seller commission and cart cleanup
     * within a single transaction.
     */
    suspend fun placeOrder(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): List<OrderResponse>

    /**
     * Resolves the authorization facts for order placement relative to
     * [userId]. Throws if the shipping address does not exist.
     */
    suspend fun getPlaceOrderAccess(
        userId: String,
        shippingAddressId: String,
    ): PlaceOrderAccess

    /**
     * Calculates the checkout summary without placing an order.
     */
    suspend fun getCheckoutSummary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutSummaryResponse

    /**
     * Creates a new order from a direct order request. Assumes the caller has
     * already authorized and validated the request.
     */
    suspend fun createOrder(
        userId: String,
        orderRequest: OrderRequest,
        idempotencyKey: String? = null,
    ): List<OrderResponse>

    /**
     * Resolves the authorization facts for an order relative to [userId].
     */
    suspend fun getOrderAccess(
        userId: String,
        orderId: String,
    ): OrderAccess

    /**
     * Applies a status transition (with cancellation side effects if CANCELED)
     * and records a status history entry. The caller is responsible for
     * authorization and transition-rule enforcement.
     */
    suspend fun applyStatusTransition(
        orderId: String,
        status: OrderStatus,
        changedBy: String,
    ): OrderResponse

    /**
     * Cancels an order and restores stock quantities. The caller is responsible
     * for authorization and cancelability checks.
     */
    suspend fun cancelOrder(
        orderId: String,
        reason: String,
        changedBy: String,
    ): OrderResponse

    /**
     * Resolves the email of a user (for after-commit notifications).
     */
    suspend fun getUserEmail(userId: String): String?

    /**
     * Retrieves a list of orders for a user.
     */
    suspend fun getOrders(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse>

    /**
     * Retrieves orders for a seller's shop.
     */
    suspend fun getSellerOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: OrderStatus?,
    ): PaginatedResponse<OrderResponse>

    /**
     * Retrieves all orders with optional filters for admin.
     */
    suspend fun getAdminOrders(
        limit: Int,
        offset: Int,
        status: OrderStatus?,
        startDate: Instant?,
        endDate: Instant?,
    ): PaginatedResponse<OrderResponse>
}
