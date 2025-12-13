package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.request.OrderRequest
import com.piashcse.model.response.Order
import com.piashcse.utils.ValidationException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import java.util.UUID

/**
 * Controller for managing order-related operations.
 */
class OrderService : OrderRepository {

    /**
     * Creates a new order for a user and stores the associated order items.
     * Orders are split by Shop (Multi-Vendor).
     *
     * @param userId The ID of the user placing the order.
     * @param orderRequest The details of the order.
     * @return The list of created order entities.
     * @throws ValidationException if validation fails or stock is insufficient.
     */
    override suspend fun createOrder(userId: String, orderRequest: OrderRequest): List<Order> = query {
        orderRequest.validation()
        if (userId.isBlank()) throw ValidationException("User ID cannot be blank")
        if (orderRequest.orderItems.isEmpty()) throw ValidationException("Order must contain at least one item")

        // 1. Fetch all products and validate existence and stock
        val productIds = orderRequest.orderItems.map { it.productId }.distinct()
        // Map productId -> ProductDAO
        val productsMap = ProductDAO.find { ProductTable.id inList productIds }
            .associateBy { it.id.value }

        // Validate all products found
        orderRequest.orderItems.forEach { item ->
            val product = productsMap[item.productId]
                ?: throw ValidationException("Product with ID ${item.productId} not found")
            
            if (product.stockQuantity < item.quantity) {
                throw ValidationException("Insufficient stock for product: ${product.name}. Available: ${product.stockQuantity}")
            }
            if (product.shopId == null) {
                 throw ValidationException("Product ${product.name} does not belong to any shop.")
            }
        }

        // 2. Group items by Shop ID
        val itemsByShop = orderRequest.orderItems.groupBy { 
            productsMap[it.productId]!!.shopId!!.value 
        }

        val createdOrders = mutableListOf<OrderDAO>()

        // 3. Create Order per Shop
        itemsByShop.forEach { (shopId, items) ->
            // Calculate totals for this shop's order
            var shopSubTotal = 0.0
            // Assuming no tax/shipping calculation logic exists yet, defaulting to 0 or proportional if provided in request?
            // Safer to default 0 and let Admin/Seller update, or implement Shipping Service. 
            // We'll trust calculated price from DB.
            
            // Create Order first
            val order = OrderDAO.new {
                this.userId = EntityID(userId, UserTable)
                this.shopId = EntityID(shopId, ShopTable)
                this.orderNumber = UUID.randomUUID().toString() // Generate unique order number
                this.status = OrderStatus.PENDING
                this.paymentStatus = com.piashcse.constants.PaymentStatus.PENDING
                this.subTotal = java.math.BigDecimal.ZERO // Will update after items
                this.total = java.math.BigDecimal.ZERO
                this.shippingAddress = orderRequest.shippingAddress
            }

            var orderSubTotalVal = 0.0

            items.forEach { itemRequest ->
                val product = productsMap[itemRequest.productId]!!
                val itemTotal = product.price.toDouble() * itemRequest.quantity
                
                // Create OrderItem
                OrderItemDAO.new {
                    this.orderId = order.id
                    this.productId = product.id
                    this.shopId = EntityID(shopId, ShopTable)
                    this.quantity = itemRequest.quantity
                    this.price = product.price
                    this.total = java.math.BigDecimal.valueOf(itemTotal)
                    this.sku = product.sku
                    this.productName = product.name
                    this.taxAmount = java.math.BigDecimal.ZERO
                    this.discountAmount = java.math.BigDecimal.ZERO
                    
                    // Update Stock
                    product.stockQuantity -= itemRequest.quantity
                }
                
                orderSubTotalVal += itemTotal
            }
            
            // Update Order totals
            order.subTotal = java.math.BigDecimal.valueOf(orderSubTotalVal)
            order.total = java.math.BigDecimal.valueOf(orderSubTotalVal) // Add valid tax/shipping here if needed
            
            createdOrders.add(order)
        }

        // 4. Clear items from cart (Bulk delete would be better but iterating is fine for now)
        orderRequest.orderItems.forEach { orderItem ->
            val cartItem = CartItemDAO.find {
                CartItemTable.userId eq userId and (CartItemTable.productId eq orderItem.productId)
            }.singleOrNull()
            cartItem?.delete()
        }

        createdOrders.map { it.response() }
    }

    /**
     * Retrieves a list of orders for a user with a specified limit.
     *
     * @param userId The ID of the user for whom to retrieve orders.
     * @param limit The maximum number of orders to retrieve.
     * @return A list of order entities for the user.
     */
    override suspend fun getOrders(userId: String, limit: Int): List<Order> = query {
        OrderDAO.find { OrderTable.userId eq userId }.limit(limit).map {
            it.response()
        }
    }

    /**
     * Updates the status of a user's order.
     *
     * @param userId The ID of the user whose order status is being updated.
     * @param orderId The ID of the order to be updated.
     * @param status The new status of the order.
     * @return The updated order entity with the new status.
     * @throws Exception if the order does not exist for the given user.
     */
    override suspend fun updateOrderStatus(userId: String, orderId: String, status: OrderStatus): Order = query {
        if (userId.isBlank()) throw ValidationException("User ID cannot be blank")
        if (orderId.isBlank()) throw ValidationException("Order ID cannot be blank")

        val order = OrderDAO.findById(orderId) ?: throw ValidationException("Order not found")

        // Fetch User to check role and permissions
        val user = UserDAO.findById(userId) ?: throw ValidationException("User not found")
        
        // 1. Check if user is the Customer
        val isCustomer = order.userId.value == userId

        // 2. Check if user is the Seller (Owner of the Shop)
        // Seller links User and Shop.
        var isSeller = false
        if (order.shopId != null) {
            val seller = SellerDAO.find { SellerTable.userId eq userId }.singleOrNull()
            if (seller != null && seller.shopId?.value == order.shopId?.value) {
                isSeller = true
            }
        }

        // 3. Check if Admin
        val isAdmin = user.userType == com.piashcse.constants.UserType.ADMIN || 
                      user.userType == com.piashcse.constants.UserType.SUPER_ADMIN

        if (!isCustomer && !isSeller && !isAdmin) {
            throw ValidationException("You are not authorized to update this order")
        }

        // Note: Specific status transitions (e.g. only Seller can mark DELIVERED) are handled in Routes or can be added here.
        // For now, access is validated.

        order.status = status
        order.response()
    }
}