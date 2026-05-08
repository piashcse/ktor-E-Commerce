package com.piashcse.feature.order

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.database.entities.*
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.OrderRequest
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class OrderService : OrderRepository {
    /**
     * Places a new order from the user's cart.
     *
     * @param userId The ID of the user placing the order.
     * @param checkoutRequest The checkout details.
     * @return The list of created order responses.
     */
    override suspend fun placeOrder(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): List<OrderResponse> =
        query {
            checkoutRequest.validation()

            // 0. Check for duplicate idempotency key
            checkoutRequest.idempotencyKey?.let { key ->
                val existingOrders = OrderDAO.find { OrderTable.idempotencyKey eq key }.toList()
                if (existingOrders.isNotEmpty()) {
                    return@query existingOrders.map { it.response() }
                }
            }

            // 1. Fetch Cart Items
            val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
            if (cartItems.isEmpty()) throw ValidationException(Message.Cart.EMPTY_CART)

            // 2. Fetch Shipping Address
            val shippingAddress =
                ShippingAddressDAO.findById(checkoutRequest.shippingAddressId)
                    ?: throw ValidationException("Shipping address not found")
            if (shippingAddress.userId.value != userId) throw ValidationException("Unauthorized shipping address")

            val fullAddress =
                "${shippingAddress.firstName} ${shippingAddress.lastName}\n" +
                    "${shippingAddress.streetAddress}, ${shippingAddress.city}, ${shippingAddress.state ?: ""}\n" +
                    "${shippingAddress.country}, ${shippingAddress.zipCode}\n" +
                    "Phone: ${shippingAddress.phoneNumber}"

            // 3. Fetch Shipping Method
            val shippingMethod =
                ShippingMethodDAO.findById(checkoutRequest.shippingMethodId)
                    ?: throw ValidationException("Shipping method not found")

            // 4. Validate Stock and Calculate Totals
            val productIds = cartItems.map { it.productId.value }.distinct()
            val productsMap =
                ProductDAO.find { ProductTable.id inList productIds }
                    .associateBy { it.id.value }

            // Group items by Shop
            val itemsByShop =
                cartItems.groupBy {
                    productsMap[it.productId.value]?.shopId?.value
                        ?: throw ValidationException("Product ${it.productId.value} does not belong to any shop")
                }

            val createdOrders = mutableListOf<OrderDAO>()
            val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            val orderCountForDate = getOrderCountForDate(today)

            itemsByShop.entries.forEachIndexed { index, (shopIdValue, items) ->
                val sequenceNumber = orderCountForDate + index + 1
                val orderNumber = "ORD-$today-${sequenceNumber.toString().padStart(4, '0')}"

                var shopSubTotal = BigDecimal.ZERO

                val order =
                    OrderDAO.new {
                        this.userId = EntityID(userId, UserTable)
                        this.shopId = EntityID(shopIdValue, ShopTable)
                        this.orderNumber = orderNumber
                        this.idempotencyKey = checkoutRequest.idempotencyKey
                        this.status = OrderStatus.PENDING
                        this.paymentStatus = com.piashcse.constants.PaymentStatus.PENDING
                        this.subTotal = BigDecimal.ZERO
                        this.shippingCost = BigDecimal(shippingMethod.price.toString())
                        this.shippingMethod = shippingMethod.name
                        this.total = BigDecimal.ZERO
                        this.shippingAddress = fullAddress
                        this.paymentMethod = checkoutRequest.paymentMethod
                        this.notes = checkoutRequest.notes
                    }

                items.forEach { cartItem ->
                    val product = productsMap[cartItem.productId.value]!!
                    val effectiveStock = getEffectiveStockQuantity(product)
                    if (effectiveStock < cartItem.quantity) {
                        throw ValidationException(Message.Validation.insufficientStock(product.name, effectiveStock))
                    }

                    val unitPrice = product.discountPrice ?: product.price
                    val itemTotal = unitPrice.multiply(BigDecimal(cartItem.quantity))

                    OrderItemDAO.new {
                        this.orderId = order.id
                        this.productId = product.id
                        this.shopId = EntityID(shopIdValue, ShopTable)
                        this.quantity = cartItem.quantity
                        this.price = unitPrice
                        this.total = itemTotal
                        this.sku = product.sku
                        this.productName = product.name
                        this.taxAmount = BigDecimal.ZERO
                        this.discountAmount = BigDecimal.ZERO
                    }

                    updateEffectiveStock(product, cartItem.quantity)
                    shopSubTotal = shopSubTotal.add(itemTotal)
                }

                val taxAmount = shopSubTotal.multiply(BigDecimal(com.piashcse.constants.AppConstants.DEFAULT_TAX_PERCENTAGE.toString()))
                order.subTotal = shopSubTotal
                order.taxAmount = taxAmount.setScale(2, java.math.RoundingMode.HALF_UP)
                order.total = shopSubTotal.add(order.shippingCost).add(order.taxAmount)
                createdOrders.add(order)
            }

            // Apply Coupon if present
            if (checkoutRequest.couponCode != null) {
                val totalSubTotal = createdOrders.map { it.subTotal }.reduce { acc, bigDecimal -> acc.add(bigDecimal) }
                val discount = validateAndApplyCoupon(checkoutRequest.couponCode, totalSubTotal.toDouble())

                // Distribute discount proportionally across orders if multi-shop
                createdOrders.forEach { order ->
                    val proportion = order.subTotal.divide(totalSubTotal, 10, java.math.RoundingMode.HALF_UP)
                    val orderDiscount = discount.multiply(proportion).setScale(2, java.math.RoundingMode.HALF_UP)
                    order.discountAmount = orderDiscount
                    order.couponCode = checkoutRequest.couponCode
                    order.total = order.total.subtract(orderDiscount)
                }
            }

            // 5. Clear Cart
            cartItems.forEach { it.delete() }

            createdOrders.forEach { order ->
                logStatusChange(order.id.value, order.status, "Order placed", userId)
            }

            createdOrders.map { it.response() }
        }

    private fun logStatusChange(
        orderId: String,
        status: OrderStatus,
        notes: String?,
        userId: String?,
    ) {
        OrderStatusHistoryDAO.new {
            this.orderId = EntityID(orderId, OrderTable)
            this.status = status
            this.notes = notes
            this.changedBy = userId?.let { EntityID(it, UserTable) }
        }
    }

    override suspend fun getCheckoutSummary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): com.piashcse.model.response.CheckoutSummaryResponse =
        query {
            checkoutRequest.validation()

            val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
            if (cartItems.isEmpty()) throw ValidationException(Message.Cart.EMPTY_CART)

            val shippingMethod =
                ShippingMethodDAO.findById(checkoutRequest.shippingMethodId)
                    ?: throw ValidationException("Shipping method not found")

            val productIds = cartItems.map { it.productId.value }.distinct()
            val productsMap =
                ProductDAO.find { ProductTable.id inList productIds }
                    .associateBy { it.id.value }

            var subTotal = BigDecimal.ZERO
            var totalItems = 0

            cartItems.forEach { cartItem ->
                val product = productsMap[cartItem.productId.value]!!
                val unitPrice = product.discountPrice ?: product.price
                subTotal = subTotal.add(unitPrice.multiply(BigDecimal(cartItem.quantity)))
                totalItems += cartItem.quantity
            }

            val taxAmount = subTotal.multiply(BigDecimal(com.piashcse.constants.AppConstants.DEFAULT_TAX_PERCENTAGE.toString()))
            var response =
                com.piashcse.model.response.CheckoutSummaryResponse(
                    subTotal = subTotal.toFloat(),
                    shippingCost = shippingMethod.price.toFloat(),
                    taxAmount = taxAmount.setScale(2, java.math.RoundingMode.HALF_UP).toFloat(),
                    total = subTotal.add(BigDecimal(shippingMethod.price.toString())).add(taxAmount).toFloat(),
                    itemCount = totalItems,
                )

            if (checkoutRequest.couponCode != null) {
                val discount = validateAndApplyCoupon(checkoutRequest.couponCode, subTotal.toDouble())
                response =
                    response.copy(
                        discountAmount = discount.toFloat(),
                        total = response.total - discount.toFloat(),
                    )
            }
            response
        }

    private fun validateAndApplyCoupon(
        code: String,
        orderAmount: Double,
    ): BigDecimal {
        val coupon =
            CouponDAO.find { CouponTable.code eq code and (CouponTable.isActive eq true) }.firstOrNull()
                ?: throw ValidationException("Invalid or inactive coupon code")

        val now = java.time.LocalDateTime.now()
        if (now.isBefore(coupon.startDate) || now.isAfter(coupon.endDate)) {
            throw ValidationException("Coupon is expired or not yet active")
        }

        if (orderAmount < coupon.minOrderAmount) {
            throw ValidationException("Order amount is below the minimum required for this coupon ($${coupon.minOrderAmount})")
        }

        if (coupon.usageLimit != null && coupon.usageCount >= coupon.usageLimit!!) {
            throw ValidationException("Coupon usage limit reached")
        }

        val discountAmount =
            when (coupon.discountType.uppercase()) {
                "PERCENTAGE" -> {
                    var amount = BigDecimal(orderAmount).multiply(BigDecimal(coupon.discountValue / 100.0))
                    coupon.maxDiscountAmount?.let { max ->
                        amount = amount.min(BigDecimal(max))
                    }
                    amount
                }
                "FIXED" -> BigDecimal(coupon.discountValue)
                else -> BigDecimal.ZERO
            }

        // Update usage count
        coupon.usageCount += 1

        return discountAmount.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Creates a new order for a user and stores the associated order items.
     * Orders are split by Shop (Multi-Vendor).
     *
     * @param userId The ID of the user placing the order.
     * @param orderRequest The details of the order.
     * @param idempotencyKey Optional key to prevent duplicate order creation.
     * @return The list of created order entities.
     * @throws ValidationException if validation fails or stock is insufficient.
     */
    override suspend fun createOrder(
        userId: String,
        orderRequest: OrderRequest,
        idempotencyKey: String?,
    ): List<OrderResponse> =
        query {
            orderRequest.validation()
            if (userId.isBlank()) throw ValidationException(Message.Validation.blankField("User ID"))
            if (orderRequest.orderItems.isEmpty()) throw ValidationException(Message.Validation.INVALID_ORDER_ITEMS)

            // Check for duplicate idempotency key
            idempotencyKey?.let { key ->
                val existingOrder = OrderDAO.find { OrderTable.idempotencyKey eq key }.firstOrNull()
                if (existingOrder != null) {
                    return@query listOf(existingOrder.response())
                }
            }

            // 1. Fetch all products and validate existence and stock
            val productIds = orderRequest.orderItems.map { it.productId }.distinct()
            // Map productId -> ProductDAO
            val productsMap =
                ProductDAO.find { ProductTable.id inList productIds }
                    .associateBy { it.id.value }

            // Validate all products found and calculate expected total
            var calculatedSubtotal = BigDecimal.ZERO

            orderRequest.orderItems.forEach { item ->
                val product =
                    productsMap[item.productId]
                        ?: throw ValidationException(Message.Validation.productNotFound(item.productId))

                // Use effective stock (inventory if exists, otherwise product stock)
                val effectiveStock = getEffectiveStockQuantity(product)
                if (effectiveStock < item.quantity) {
                    throw ValidationException(Message.Validation.insufficientStock(product.name, effectiveStock))
                }
                if (product.shopId == null) {
                    throw ValidationException(Message.Orders.productDoesNotBelongToShop(product.name))
                }

                // Calculate price from DB (use discountPrice if available, otherwise price)
                val unitPrice = product.discountPrice ?: product.price
                val itemTotal = unitPrice.multiply(BigDecimal(item.quantity))
                calculatedSubtotal = calculatedSubtotal.add(itemTotal)
            }

            // Validate order total matches calculated total
            val requestTotal = BigDecimal(orderRequest.total.toString())
            if (requestTotal.compareTo(calculatedSubtotal) != 0) {
                throw ValidationException(
                    "OrderResponse total mismatch. Requested: ${orderRequest.total}, Calculated: $calculatedSubtotal",
                )
            }

            // 2. Group items by Shop ID
            val itemsByShop =
                orderRequest.orderItems.groupBy {
                    productsMap[it.productId]!!.shopId!!.value
                }

            val createdOrders = mutableListOf<OrderDAO>()

            // Generate human-readable order number prefix
            val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) // YYYYMMDD
            val orderCountForDate = getOrderCountForDate(today)

            // 3. Create OrderResponse per Shop
            itemsByShop.entries.forEachIndexed { index, (shopIdValue, items) ->
                // Generate order number: ORD-YYYYMMDD-XXXX
                val sequenceNumber = orderCountForDate + index + 1
                val orderNumber = "ORD-$today-${sequenceNumber.toString().padStart(4, '0')}"

                // Calculate totals for this shop's order
                var shopSubTotal = BigDecimal.ZERO

                // Create OrderResponse first
                val order =
                    OrderDAO.new {
                        this.userId = EntityID(userId, UserTable)
                        this.shopId = EntityID(shopIdValue, ShopTable)
                        this.orderNumber = orderNumber
                        this.idempotencyKey = idempotencyKey
                        this.status = OrderStatus.PENDING
                        this.paymentStatus = com.piashcse.constants.PaymentStatus.PENDING
                        this.subTotal = BigDecimal.ZERO // Will update after items
                        this.total = BigDecimal.ZERO
                        this.shippingAddress = orderRequest.shippingAddress
                    }

                items.forEach { itemRequest ->
                    val product = productsMap[itemRequest.productId]!!
                    val unitPrice = product.discountPrice ?: product.price
                    val itemTotal = unitPrice.multiply(BigDecimal(itemRequest.quantity))

                    // Create OrderItem
                    OrderItemDAO.new {
                        this.orderId = order.id
                        this.productId = product.id
                        this.shopId = EntityID(shopIdValue, ShopTable)
                        this.quantity = itemRequest.quantity
                        this.price = unitPrice
                        this.total = itemTotal
                        this.sku = product.sku
                        this.productName = product.name
                        this.taxAmount = BigDecimal.ZERO
                        this.discountAmount = BigDecimal.ZERO

                        // Update effective stock
                        updateEffectiveStock(product, itemRequest.quantity)
                    }

                    shopSubTotal = shopSubTotal.add(itemTotal)
                }

                // Update OrderResponse totals
                order.subTotal = shopSubTotal
                order.total = shopSubTotal // Add tax/shipping here if needed

                createdOrders.add(order)
            }

            // 4. Clear items from cart
            orderRequest.orderItems.forEach { orderItem ->
                val cartItem =
                    CartItemDAO.find {
                        CartItemTable.userId eq userId and (CartItemTable.productId eq orderItem.productId)
                    }.singleOrNull()
                cartItem?.delete()
            }

            createdOrders.map { it.response() }
        }

    /**
     * Returns the effective stock quantity for a product.
     * If an inventory record exists, uses inventory.stockQuantity; otherwise uses product.stockQuantity.
     */
    private fun getEffectiveStockQuantity(product: ProductDAO): Int {
        val inventory =
            InventoryDAO.find {
                InventoryTable.productId eq product.id
            }.firstOrNull()

        return inventory?.stockQuantity ?: product.stockQuantity
    }

    /**
     * Updates the effective stock quantity for a product after an order.
     */
    private fun updateEffectiveStock(
        product: ProductDAO,
        quantityOrdered: Int,
    ) {
        val inventory =
            InventoryDAO.find {
                InventoryTable.productId eq product.id
            }.firstOrNull()

        if (inventory != null) {
            // Update inventory stock
            val newStock = (inventory.stockQuantity - quantityOrdered).coerceAtLeast(0)
            inventory.stockQuantity = newStock
            // Update inventory status
            inventory.status =
                com.piashcse.constants.InventoryStatus.valueOf(
                    when {
                        newStock == 0 -> "OUT_OF_STOCK"
                        newStock <= inventory.minimumStockLevel -> "LOW_STOCK"
                        else -> "IN_STOCK"
                    },
                )
        } else {
            // Update product stock directly
            val newStock = (product.stockQuantity - quantityOrdered).coerceAtLeast(0)
            product.stockQuantity = newStock
        }
    }

    /**
     * Gets the count of orders for a given date prefix to generate sequential order numbers.
     */
    private fun getOrderCountForDate(datePrefix: String): Int {
        return OrderDAO.find {
            OrderTable.orderNumber like "ORD-$datePrefix%"
        }.count().toInt()
    }

    /**
     * Retrieves a list of orders for a user with a specified limit.
     *
     * @param userId The ID of the user for whom to retrieve orders.
     * @param limit The maximum number of orders to retrieve.
     * @return A list of order entities for the user.
     */
    override suspend fun getOrders(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> =
        query {
            OrderTable.selectAll().andWhere { OrderTable.userId eq userId }
                .orderBy(OrderTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    OrderDAO.wrapRow(it).response()
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
    override suspend fun updateOrderStatus(
        userId: String,
        orderId: String,
        status: OrderStatus,
    ): OrderResponse =
        query {
            if (userId.isBlank()) throw ValidationException(Message.Validation.blankField("User ID"))
            if (orderId.isBlank()) throw ValidationException(Message.Validation.blankField("OrderResponse ID"))

            val order = OrderDAO.findById(orderId) ?: throw ValidationException(Message.Orders.NOT_FOUND)

            // Fetch User to check role and permissions
            val user = UserDAO.findById(userId) ?: throw ValidationException(Message.Errors.NOT_FOUND)

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
            val isAdmin =
                user.userType == com.piashcse.constants.UserType.ADMIN ||
                    user.userType == com.piashcse.constants.UserType.SUPER_ADMIN

            if (!isCustomer && !isSeller && !isAdmin) {
                throw ValidationException(Message.Orders.UNAUTHORIZED)
            }

            // Note: Specific status transitions (e.g. only Seller can mark DELIVERED) are handled in Routes or can be added here.
            // For now, access is validated.

            order.status = status
            logStatusChange(order.id.value, status, "Status updated by user", userId)
            order.response()
        }

    /**
     * Cancels an order and restores stock quantities.
     *
     * @param orderId The ID of the order to cancel.
     * @param userId The ID of the user requesting cancellation.
     * @param reason The reason for cancellation.
     * @param userType The type of user (CUSTOMER, SELLER, ADMIN, SUPER_ADMIN).
     * @return The updated order with CANCELED status.
     */
    override suspend fun cancelOrder(
        orderId: String,
        userId: String,
        reason: String,
        userType: com.piashcse.constants.UserType,
    ): OrderResponse =
        query {
            if (orderId.isBlank()) throw ValidationException(Message.Validation.blankField("OrderResponse ID"))
            if (reason.isBlank()) throw ValidationException(Message.Orders.CANCEL_REASON_REQUIRED)

            val order =
                OrderDAO.findById(orderId)
                    ?: throw ValidationException(Message.Orders.NOT_FOUND)

            // Permission check
            val isCustomer = order.userId.value == userId
            val isSeller =
                order.shopId?.value?.let { shopId ->
                    SellerDAO.find { SellerTable.userId eq userId }.singleOrNull()?.shopId?.value == shopId
                } == true
            val isAdmin =
                userType in
                    listOf(
                        com.piashcse.constants.UserType.ADMIN,
                        com.piashcse.constants.UserType.SUPER_ADMIN,
                    )

            if (!isCustomer && !isSeller && !isAdmin) {
                throw ValidationException(Message.Orders.UNAUTHORIZED)
            }

            // Customers can only cancel their own orders
            if (isCustomer && !isSeller && !isAdmin && order.userId.value != userId) {
                throw ValidationException(Message.Orders.UNAUTHORIZED)
            }

            // Status check — only PENDING or CONFIRMED can be cancelled
            if (!OrderStatus.canBeCanceled(order.status)) {
                throw ValidationException(Message.Orders.CANNOT_CANCEL)
            }

            val oldStatus = order.status
            order.status = OrderStatus.CANCELED
            order.canceledDate = java.time.LocalDateTime.now()
            order.notes = reason
            logStatusChange(order.id.value, OrderStatus.CANCELED, reason, userId)

            // Restore stock
            OrderItemDAO.find { OrderItemTable.orderId eq order.id }.forEach { orderItem ->
                val product = ProductDAO.findById(orderItem.productId.value)
                product?.let {
                    restoreEffectiveStock(it, orderItem.quantity)
                }
            }

            order.response()
        }

    /**
     * Restores the effective stock quantity for a product after order cancellation.
     */
    private fun restoreEffectiveStock(
        product: ProductDAO,
        quantityToRestore: Int,
    ) {
        val inventory =
            InventoryDAO.find {
                InventoryTable.productId eq product.id
            }.firstOrNull()

        if (inventory != null) {
            // Restore inventory stock
            val newStock = inventory.stockQuantity + quantityToRestore
            inventory.stockQuantity = newStock
            // Update inventory status
            inventory.status =
                com.piashcse.constants.InventoryStatus.valueOf(
                    when {
                        newStock == 0 -> "OUT_OF_STOCK"
                        newStock <= inventory.minimumStockLevel -> "LOW_STOCK"
                        else -> "IN_STOCK"
                    },
                )
        } else {
            // Restore product stock
            product.stockQuantity = product.stockQuantity + quantityToRestore
        }
    }

    /**
     * Retrieves orders for a seller's shop.
     *
     * @param userId The ID of the seller.
     * @param limit The maximum number of orders to return.
     * @param offset The offset for pagination.
     * @param status Optional status filter.
     * @return A list of orders for the seller's shop.
     */
    override suspend fun getSellerOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
    ): PaginatedResponse<OrderResponse> =
        query {
            val seller =
                SellerDAO.find { SellerTable.userId eq userId }.firstOrNull()
                    ?: throw ValidationException("Seller profile not found")

            val shopId =
                seller.shopId
                    ?: throw ValidationException("No shop associated with seller")

            val query = OrderTable.selectAll().andWhere { OrderTable.shopId eq shopId }

            if (status != null) {
                query.andWhere { OrderTable.status eq OrderStatus.valueOf(status.uppercase()) }
            }

            query.orderBy(OrderTable.createdAt to SortOrder.DESC).toPaginatedResponse(limit, offset) {
                OrderDAO.wrapRow(it).response()
            }
        }

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
    override suspend fun getAdminOrders(
        limit: Int,
        offset: Int,
        status: String?,
        startDate: java.time.Instant?,
        endDate: java.time.Instant?,
    ): PaginatedResponse<OrderResponse> =
        query {
            val query = OrderTable.selectAll()

            status?.let {
                query.andWhere { OrderTable.status eq OrderStatus.valueOf(it.uppercase()) }
            }
            startDate?.let {
                val localDateTime = java.time.LocalDateTime.ofInstant(it, java.time.ZoneOffset.UTC)
                query.andWhere { OrderTable.createdAt greaterEq localDateTime }
            }
            endDate?.let {
                val localDateTime = java.time.LocalDateTime.ofInstant(it, java.time.ZoneOffset.UTC)
                query.andWhere { OrderTable.createdAt lessEq localDateTime }
            }

            query.orderBy(OrderTable.createdAt to SortOrder.DESC).toPaginatedResponse(limit, offset) {
                OrderDAO.wrapRow(it).response()
            }
        }
}
