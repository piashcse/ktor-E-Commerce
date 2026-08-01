package com.piashcse.feature.order

import com.piashcse.constants.*
import com.piashcse.database.entities.*
import com.piashcse.event.EventBus
import com.piashcse.event.OrderPlacedEvent
import com.piashcse.mapper.toOrderItemResponse
import com.piashcse.mapper.toOrderResponse
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.OrderRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderItemResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.extension.*
import com.piashcse.utils.money.Money
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class OrderRepositoryImpl : OrderRepository {

    private fun List<OrderItemDAO>.toItemResponses() = map { it.toOrderItemResponse() }

    private fun loadItemsForOrders(orders: List<OrderDAO>): Map<String, List<OrderItemResponse>> =
        OrderItemDAO.itemsForOrders(orders.map { it.id })
            .mapValues { (_, items) -> items.toItemResponses() }

    private fun Query.toOrdersPaginated(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> {
        val (totalCount, rows) = toPaginatedList(limit, offset) { OrderDAO.wrapRow(it) }
        val itemsMap = loadItemsForOrders(rows)
        val data = rows.map { order -> order.toOrderResponse(itemsMap[order.id.value]) }
        return PaginatedResponse(data, PaginationMetadata(totalCount, limit, offset))
    }

    private fun validateShopsApproved(shopIds: Set<String>) {
        val shops = if (shopIds.isNotEmpty()) {
            ShopDAO.find { ShopTable.id inList shopIds.map { it.entityID(ShopTable) } }.associateBy { it.id.value }
        } else {
            emptyMap()
        }
        shopIds.forEach { shopId ->
            val shop = shops[shopId] ?: throw ValidationException(Message.Orders.SHOP_NOT_FOUND)
            if (shop.status != ShopStatus.APPROVED)
                throw ValidationException(Message.Orders.SHOP_INACTIVE)
        }
    }

    private fun generateOrderNumber(datePrefix: String, sequenceNumber: Int) =
        "ORD-$datePrefix-${sequenceNumber.toString().padStart(4, '0')}-${UUID.randomUUID().toString().take(8).uppercase()}"

    private fun logStatusChange(orderId: String, status: OrderStatus, notes: String?, userId: String?) {
        OrderStatusHistoryDAO.new {
            this.orderId = orderId.entityID(OrderTable)
            this.status = status
            this.notes = notes
            this.changedBy = userId?.let { it.entityID(UserTable) }
        }
    }

    private fun validateCoupon(code: String, orderAmount: BigDecimal, forUpdate: Boolean = false): CouponDAO {
        val query = CouponDAO.find { CouponTable.code eq code and (CouponTable.isActive eq true) }
        val coupon = (if (forUpdate) query.forUpdate() else query).firstOrNull()
            ?: throw ValidationException(Message.Orders.INVALID_COUPON)

        val now = LocalDateTime.now(ZoneOffset.UTC)
        if (now.isBefore(coupon.startDate) || now.isAfter(coupon.endDate))
            throw ValidationException(Message.Orders.COUPON_EXPIRED)

        if (orderAmount < coupon.minOrderAmount)
            throw ValidationException(Message.Orders.couponMinOrderAmount(coupon.minOrderAmount.toPlainString()))

        if (coupon.usageLimit != null && coupon.usageCount >= coupon.usageLimit!!)
            throw ValidationException(Message.Orders.COUPON_LIMIT_REACHED)

        return coupon
    }

    private fun calculateCouponDiscount(coupon: CouponDAO, orderAmount: BigDecimal): BigDecimal {
        val discount = when (coupon.discountType) {
            CouponDiscountType.PERCENTAGE -> {
                val percentage = coupon.discountValue.divide(BigDecimal(100), 10, RoundingMode.HALF_UP)
                val amount = orderAmount.multiply(percentage)
                coupon.maxDiscountAmount?.let { amount.min(it) } ?: amount
            }
            CouponDiscountType.FIXED -> coupon.discountValue
        }
        return Money.round(discount.min(orderAmount))
    }

    private fun consumeCoupon(
        coupon: CouponDAO,
        orderAmount: BigDecimal,
        userId: String,
        orders: List<OrderDAO>,
    ): BigDecimal {
        coupon.usageCount += 1
        orders.forEach { order ->
            CouponUsageDAO.new {
                this.couponId = coupon.id
                this.userId = userId.entityID(UserTable)
                this.orderId = order.id
                this.usedAt = LocalDateTime.now(ZoneOffset.UTC)
            }
        }
        return calculateCouponDiscount(coupon, orderAmount)
    }

    override suspend fun placeOrder(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): List<OrderResponse> = retryQuery {
        checkoutRequest.idempotencyKey?.let { key ->
            val existing = OrderDAO.find { (OrderTable.idempotencyKey eq key) and (OrderTable.userId eq userId) }.toList()
            if (existing.isNotEmpty()) {
                val itemsMap = loadItemsForOrders(existing)
                return@retryQuery existing.map { it.toOrderResponse(itemsMap[it.id.value]) }
            }
        }

        val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
        if (cartItems.isEmpty()) throw ValidationException(Message.Cart.EMPTY_CART)

        val shippingAddress = ShippingAddressDAO.findById(checkoutRequest.shippingAddressId)
            ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
        if (shippingAddress.userId.value != userId) throw ValidationException(Message.Orders.SHIPPING_ADDRESS_UNAUTHORIZED)

        val fullAddress = buildString {
            append("${shippingAddress.firstName} ${shippingAddress.lastName}\n")
            append("${shippingAddress.streetAddress}, ${shippingAddress.city}, ${shippingAddress.state ?: ""}\n")
            append("${shippingAddress.country}, ${shippingAddress.zipCode}\n")
            append("Phone: ${shippingAddress.phoneNumber}")
        }

        val shippingMethod = ShippingMethodDAO.findById(checkoutRequest.shippingMethodId)
            ?: throw ValidationException(Message.Orders.SHIPPING_METHOD_NOT_FOUND)

        val productsMap = ProductDAO.find {
            ProductTable.id inList cartItems.map { it.productId.value }.distinct()
        }.associateBy { it.id.value }

        val itemsByShop = cartItems.groupBy {
            productsMap[it.productId.value]?.shopId?.value
                ?: throw ValidationException(Message.Orders.productDoesNotBelongToShop(it.productId.value))
        }
        validateShopsApproved(itemsByShop.keys)

        val createdOrders = mutableListOf<OrderDAO>()
        val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)

        itemsByShop.entries.forEachIndexed { index, (shopIdValue, items) ->
            val orderNumber = generateOrderNumber(today, index + 1)
            var shopSubTotal = BigDecimal.ZERO

            val order = OrderDAO.new {
                this.userId = userId.entityID(UserTable)
                this.shopId = shopIdValue.entityID(ShopTable)
                this.orderNumber = orderNumber
                this.idempotencyKey = checkoutRequest.idempotencyKey
                this.status = OrderStatus.PENDING
                this.paymentStatus = PaymentStatus.PENDING
                this.subTotal = BigDecimal.ZERO
                this.shippingCost = shippingMethod.price
                this.shippingMethod = shippingMethod.name
                this.total = BigDecimal.ZERO
                this.shippingAddress = fullAddress
                this.paymentMethod = checkoutRequest.paymentMethod
                this.notes = checkoutRequest.notes
            }

            items.forEach { cartItem ->
                val product = productsMap[cartItem.productId.value] ?: cartItem.productId.value.throwNotFound("Product")
                if (product.status != ProductStatus.ACTIVE)
                    throw ValidationException(Message.Products.OUT_OF_STOCK)
                val available = product.effectiveStock(forUpdate = true)
                if (available < cartItem.quantity)
                    throw ValidationException(Message.Validation.insufficientStock(product.name, available))

                val unitPrice = product.discountPrice ?: product.price
                val itemTotal = Money.lineTotal(unitPrice, cartItem.quantity)

                val orderItem = OrderItemDAO.new {
                    orderId = order.id
                    productId = product.id
                    shopId = shopIdValue.entityID(ShopTable)
                    quantity = cartItem.quantity
                    price = unitPrice
                    total = itemTotal
                    sku = product.sku
                    productName = product.name
                    taxAmount = BigDecimal.ZERO
                    discountAmount = BigDecimal.ZERO
                }

                product.decrementStock(cartItem.quantity)
                product.addSales(cartItem.quantity)

                StockReservationDAO.new {
                    this.orderId = order.id
                    this.orderItemId = orderItem.id
                    this.productId = product.id
                    this.shopId = shopIdValue.entityID(ShopTable)
                    this.quantity = cartItem.quantity
                    this.status = ReservationStatus.ACTIVE
                    this.expiresAt = LocalDateTime.now().plusHours(24)
                }

                shopSubTotal = shopSubTotal.add(itemTotal)
            }

            val taxAmount = Money.taxOn(shopSubTotal)
            order.subTotal = shopSubTotal
            order.taxAmount = taxAmount
            order.total = Money.total(shopSubTotal, order.shippingCost, order.taxAmount)
            createdOrders.add(order)
        }

        checkoutRequest.couponCode?.let { code ->
            val totalSubTotal = createdOrders.map { it.subTotal }.reduce(BigDecimal::add)
            val coupon = validateCoupon(code, totalSubTotal, forUpdate = true)
            val discount = consumeCoupon(coupon, totalSubTotal, userId, createdOrders)
            createdOrders.forEach { order ->
                val proportion = order.subTotal.divide(totalSubTotal, 10, RoundingMode.HALF_UP)
                val orderDiscount = Money.round(discount.multiply(proportion))
                order.discountAmount = orderDiscount
                order.couponCode = code
                order.total = order.total.subtract(orderDiscount)
            }
        }

        val shopIds = createdOrders.mapNotNull { it.shopId?.value }.distinct()
        val sellersByShop = if (shopIds.isNotEmpty()) {
            SellerDAO.find { SellerTable.shopId inList shopIds.map { it.entityID(ShopTable) } }
                .associateBy { it.shopId?.value }
        } else {
            emptyMap()
        }
        createdOrders.forEach { order ->
            order.shopId?.value?.let { shopId ->
                sellersByShop[shopId]?.let { seller ->
                    seller.totalSales = seller.totalSales.add(order.subTotal)
                    seller.totalCommission = seller.totalCommission.add(seller.calcCommission(order.subTotal))
                }
            }
        }

        cartItems.forEach { it.delete() }
        createdOrders.forEach {
            logStatusChange(it.id.value, it.status, "Order placed", userId)
            EventBus.publish(
                OrderPlacedEvent(
                    orderId = it.id.value,
                    userId = userId,
                    email = UserDAO.findById(userId)?.email.orEmpty(),
                    shopId = it.shopId?.value,
                    orderNumber = it.orderNumber,
                    total = it.total,
                ),
            )
        }
        val itemsMap = loadItemsForOrders(createdOrders)
        createdOrders.map { it.toOrderResponse(itemsMap[it.id.value]) }
    }

    override suspend fun getCheckoutSummary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutSummaryResponse = query {
        val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
        if (cartItems.isEmpty()) throw ValidationException(Message.Cart.EMPTY_CART)

        val shippingMethod = ShippingMethodDAO.findById(checkoutRequest.shippingMethodId)
            ?: throw ValidationException(Message.Orders.SHIPPING_METHOD_NOT_FOUND)

        val productsMap = ProductDAO.find {
            ProductTable.id inList cartItems.map { it.productId.value }.distinct()
        }.associateBy { it.id.value }

        val lines = mutableListOf<Pair<BigDecimal, Int>>()
        var totalItems = 0
        cartItems.forEach { cartItem ->
            val product = productsMap[cartItem.productId.value]!!
            lines += (product.discountPrice ?: product.price) to cartItem.quantity
            totalItems += cartItem.quantity
        }

        val subTotal = Money.subtotal(lines)
        val shopCount = cartItems.mapNotNull { productsMap[it.productId.value]?.shopId?.value }.distinct().size
        val shippingTotal = Money.shippingTotal(shippingMethod.price, shopCount)
        val taxAmount = Money.taxOn(subTotal)
        val baseTotal = Money.total(subTotal, shippingTotal, taxAmount)
        var response = CheckoutSummaryResponse(
            subTotal = Money.plain(subTotal),
            shippingCost = Money.plain(shippingTotal),
            taxAmount = Money.plain(taxAmount),
            total = Money.plain(baseTotal),
            itemCount = totalItems,
        )

        checkoutRequest.couponCode?.let {
            val coupon = validateCoupon(it, subTotal)
            val discount = calculateCouponDiscount(coupon, subTotal)
            response = response.copy(
                discountAmount = Money.plain(discount),
                total = Money.plain(baseTotal.subtract(discount)),
            )
        }
        response
    }

    override suspend fun createOrder(
        userId: String,
        orderRequest: OrderRequest,
        idempotencyKey: String?,
    ): List<OrderResponse> = retryQuery {
        userId.requireNotBlank("User ID")
        if (orderRequest.orderItems.isEmpty()) throw ValidationException(Message.Validation.EMPTY_ORDER_ITEMS)

        idempotencyKey?.let { key ->
            OrderDAO.find { (OrderTable.idempotencyKey eq key) and (OrderTable.userId eq userId) }.firstOrNull()
                ?.let { order -> return@retryQuery listOf(order.toOrderResponse(OrderItemDAO.itemsForOrder(order.id).toItemResponses())) }
        }

        val productsMap = ProductDAO.find {
            ProductTable.id inList orderRequest.orderItems.map { it.productId }.distinct()
        }.associateBy { it.id.value }

        var calculatedSubtotal = BigDecimal.ZERO
        orderRequest.orderItems.forEach { item ->
            val product = productsMap[item.productId]
                ?: throw ValidationException(Message.Validation.productNotFound(item.productId))
            if (product.status != ProductStatus.ACTIVE)
                throw ValidationException(Message.Products.OUT_OF_STOCK)
            val available = product.effectiveStock(forUpdate = true)
            if (available < item.quantity)
                throw ValidationException(Message.Validation.insufficientStock(product.name, available))
            if (product.shopId == null) throw ValidationException(Message.Orders.productDoesNotBelongToShop(product.name))

            val unitPrice = product.discountPrice ?: product.price
            calculatedSubtotal = calculatedSubtotal.add(Money.lineTotal(unitPrice, item.quantity))
        }

        if (orderRequest.total.compareTo(calculatedSubtotal) != 0)
            throw ValidationException(Message.Orders.TOTAL_MISMATCH)

        val itemsByShop = orderRequest.orderItems.groupBy {
            val product = productsMap[it.productId] ?: it.productId.throwNotFound("Product")
            product.shopId?.value ?: throw ValidationException(Message.Orders.productDoesNotBelongToShop(product.name))
        }
        validateShopsApproved(itemsByShop.keys)

        val createdOrders = mutableListOf<OrderDAO>()
        val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)

        itemsByShop.entries.forEachIndexed { index, (shopIdValue, items) ->
            val orderNumber = generateOrderNumber(today, index + 1)
            var shopSubTotal = BigDecimal.ZERO

            val order = OrderDAO.new {
                this.userId = userId.entityID(UserTable)
                this.shopId = shopIdValue.entityID(ShopTable)
                this.orderNumber = orderNumber
                this.idempotencyKey = idempotencyKey
                this.status = OrderStatus.PENDING
                this.paymentStatus = PaymentStatus.PENDING
                this.subTotal = BigDecimal.ZERO
                this.total = BigDecimal.ZERO
                this.shippingAddress = orderRequest.shippingAddress
            }

            items.forEach { itemRequest ->
                val product = productsMap[itemRequest.productId]!!
                val unitPrice = product.discountPrice ?: product.price
                val itemTotal = Money.lineTotal(unitPrice, itemRequest.quantity)

                OrderItemDAO.new {
                    orderId = order.id
                    productId = product.id
                    shopId = shopIdValue.entityID(ShopTable)
                    quantity = itemRequest.quantity
                    price = unitPrice
                    total = itemTotal
                    sku = product.sku
                    productName = product.name
                    taxAmount = BigDecimal.ZERO
                    discountAmount = BigDecimal.ZERO
                }

                product.decrementStock(itemRequest.quantity)
                product.addSales(itemRequest.quantity)
                shopSubTotal = shopSubTotal.add(itemTotal)
            }

            order.subTotal = shopSubTotal
            order.total = shopSubTotal
            createdOrders.add(order)
        }

        orderRequest.orderItems.forEach { orderItem ->
            CartItemDAO.find {
                CartItemTable.userId eq userId and (CartItemTable.productId eq orderItem.productId)
            }.firstOrNull()?.delete()
        }

        val itemsMap = loadItemsForOrders(createdOrders)
        createdOrders.map { it.toOrderResponse(itemsMap[it.id.value]) }
    }

    override suspend fun getOrders(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> = query {
        OrderTable.selectAll().andWhere { OrderTable.userId eq userId }
            .orderBy(OrderTable.createdAt to SortOrder.DESC)
            .toOrdersPaginated(limit, offset)
    }

    override suspend fun updateOrderStatus(
        userId: String,
        orderId: String,
        status: OrderStatus,
    ): OrderResponse = query {
        userId.requireNotBlank("User ID")
        orderId.requireNotBlank("Order ID")

        val order = OrderDAO.findById(orderId) ?: throw ValidationException(Message.Orders.NOT_FOUND)
        val user = UserDAO.findById(userId) ?: throw ValidationException(Message.Errors.NOT_FOUND)

        val isCustomer = order.userId.value == userId
        val isSeller = order.shopId?.value?.let { sellerOwnsShop(userId, it) } == true
        val isAdmin = user.userType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)

        if (!isCustomer && !isSeller && !isAdmin) throw ForbiddenException(Message.Orders.UNAUTHORIZED)

        if (!OrderStatus.canTransitionTo(order.status, status))
            throw ValidationException(Message.Orders.INVALID_STATUS)

        if (status == OrderStatus.CANCELED) {
            applyOrderCancellation(order, notes = "Status updated by user", changedBy = userId)
        } else {
            order.status = status
            when (status) {
                OrderStatus.DELIVERED -> {
                    order.deliveredDate = LocalDateTime.now()
                    if (order.shippingDate == null) order.shippingDate = order.deliveredDate
                }
                OrderStatus.RECEIVED -> order.completedDate = LocalDateTime.now()
                OrderStatus.PAID -> order.paymentStatus = PaymentStatus.COMPLETED
                else -> {}
            }
            logStatusChange(order.id.value, status, "Status updated by user", userId)
        }
        order.toOrderResponse(OrderItemDAO.itemsForOrder(order.id).toItemResponses())
    }

    override suspend fun cancelOrder(
        orderId: String,
        userId: String,
        reason: String,
        userType: UserType,
    ): OrderResponse = retryQuery {
        orderId.requireNotBlank("Order ID")
        if (reason.isBlank()) throw ValidationException(Message.Orders.CANCEL_REASON_REQUIRED)

        val order = OrderDAO.findById(orderId) ?: throw ValidationException(Message.Orders.NOT_FOUND)

        val isCustomer = order.userId.value == userId
        val isSeller = order.shopId?.value?.let { sellerOwnsShop(userId, it) } == true
        val isAdmin = userType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)

        if (!isCustomer && !isSeller && !isAdmin) throw ForbiddenException(Message.Orders.UNAUTHORIZED)
        if (!OrderStatus.canBeCanceled(order.status)) throw ValidationException(Message.Orders.CANNOT_CANCEL)

        applyOrderCancellation(order, notes = reason, changedBy = userId)
        order.toOrderResponse(OrderItemDAO.itemsForOrder(order.id).toItemResponses())
    }

    private fun applyOrderCancellation(
        order: OrderDAO,
        notes: String?,
        changedBy: String?,
    ) {
        order.status = OrderStatus.CANCELED
        order.canceledDate = LocalDateTime.now()
        order.notes = notes
        if (order.paymentStatus == PaymentStatus.COMPLETED) {
            order.paymentStatus = PaymentStatus.REFUNDED
        }
        logStatusChange(order.id.value, OrderStatus.CANCELED, notes, changedBy)

        val orderItems = OrderItemDAO.find { OrderItemTable.orderId eq order.id }.toList()
        val productIds = orderItems.map { it.productId.value }
        val productsMap = if (productIds.isNotEmpty()) {
            ProductDAO.find { ProductTable.id inList productIds }.associateBy { it.id.value }
        } else {
            emptyMap()
        }
        orderItems.forEach { orderItem ->
            productsMap[orderItem.productId.value]?.restoreStock(orderItem.quantity)
            productsMap[orderItem.productId.value]?.removeSales(orderItem.quantity)
        }

        StockReservationDAO.find { StockReservationTable.orderId eq order.id }
            .forEach { it.status = ReservationStatus.RELEASED }
    }

    override suspend fun getSellerOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
    ): PaginatedResponse<OrderResponse> = query {
        val seller = findSellerByUserId(userId) ?: throw ValidationException(Message.Orders.SELLER_PROFILE_NOT_FOUND)
        val shopId = seller.shopId ?: throw ValidationException(Message.Orders.NO_SHOP_ASSOCIATED)

        val query = OrderTable.selectAll().andWhere { OrderTable.shopId eq shopId }
        status?.let { query.andWhere { OrderTable.status eq OrderStatus.valueOf(it.uppercase()) } }
        query.orderBy(OrderTable.createdAt to SortOrder.DESC).toOrdersPaginated(limit, offset)
    }

    override suspend fun getAdminOrders(
        limit: Int,
        offset: Int,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
    ): PaginatedResponse<OrderResponse> = query {
        val query = OrderTable.selectAll()
        status?.let { query.andWhere { OrderTable.status eq OrderStatus.valueOf(it.uppercase()) } }
        startDate?.let { query.andWhere { OrderTable.createdAt greaterEq LocalDateTime.ofInstant(it, ZoneOffset.UTC) } }
        endDate?.let { query.andWhere { OrderTable.createdAt lessEq LocalDateTime.ofInstant(it, ZoneOffset.UTC) } }
        query.orderBy(OrderTable.createdAt to SortOrder.DESC).toOrdersPaginated(limit, offset)
    }
}
