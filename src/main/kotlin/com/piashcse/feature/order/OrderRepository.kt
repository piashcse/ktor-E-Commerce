package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.UserType
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.CursorPage
import com.piashcse.utils.common.PaginatedResponse
import java.time.Instant

interface OrderRepository {
    /**
     * Places a new order from the user's cart.
     *
     * @param userId The unique identifier of the user placing the order.
     * @param checkoutRequest The checkout details including address and shipping method.
     * @return The list of created orders (split by shop).
     */
    suspend fun placeOrder(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): List<OrderResponse>

    /**
     * Calculates the checkout summary without placing an order.
     *
     * @param userId The unique identifier of the user.
     * @param checkoutRequest The checkout details.
     * @return The checkout summary including totals and item count.
     */
    suspend fun getCheckoutSummary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutSummaryResponse

    /**
     * Retrieves a list of orders for a user.
     *
     * @param userId The unique identifier of the user.
     * @param limit The maximum number of orders to return.
     * @return A list of orders.
     */
    suspend fun getOrders(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse>

    /** Keyset-paginated customer orders; preferred for large histories. */
    suspend fun getOrdersCursor(
        userId: String,
        limit: Int,
        cursor: String?,
    ): CursorPage<OrderResponse>

    /** Guest order lookup by order number + checkout email. */
    suspend fun getOrderForGuest(
        orderNumber: String,
        email: String,
    ): OrderResponse?

    /**
     * Updates the status of an order.
     *
     * @param userId The unique identifier of the user.
     * @param orderId The unique identifier of the order.
     * @param status The updated order status.
     * @return The updated order.
     */
    suspend fun updateOrderStatus(
        userId: String,
        orderId: String,
        status: OrderStatus,
    ): OrderResponse

    /**
     * Cancels an order and restores stock quantities.
     *
     * @param orderId The unique identifier of the order to cancel.
     * @param userId The unique identifier of the user requesting cancellation.
     * @param reason The reason for cancellation.
     * @param userType The type of user (CUSTOMER, SELLER, ADMIN, SUPER_ADMIN).
     * @return The updated order with CANCELED status.
     */
    suspend fun cancelOrder(
        orderId: String,
        userId: String,
        reason: String,
        userType: UserType,
    ): OrderResponse

    /**
     * Marks a paid order as shipped and stores the carrier tracking number.
     */
    suspend fun shipOrder(
        orderId: String,
        userId: String,
        trackingNumber: String,
    ): OrderResponse

    /**
     * Retrieves orders for a seller's shop.
     *
     * @param userId The unique identifier of the seller.
     * @param limit The maximum number of orders to return.
     * @param offset The offset for pagination.
     * @param status Optional status filter.
     * @return A list of orders for the seller's shop.
     */
    suspend fun getSellerOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
    ): PaginatedResponse<OrderResponse>

    /**
     * Retrieves all orders with optional filters for admin.
     *
     * @param limit The maximum number of orders to return.
     * @param offset The offset for pagination.
     * @param status Optional status filter.
     * @param startDate Optional start date filter.
     * @param endDate Optional end date filter.
     * @return A pair containing the list of orders and the total count.
     */
    suspend fun getAdminOrders(
        limit: Int,
        offset: Int,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
    ): PaginatedResponse<OrderResponse>
}
