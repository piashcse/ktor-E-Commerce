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
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
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
            ShopDAO.find { ShopTable.id inList shopIds.map { EntityID(it, ShopTable) } }.associateBy { it.id.value }
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
            this.orderId = EntityID(orderId, OrderTable)
            this.status = status
            this.notes = notes
            this.changedBy = userId?.let { EntityID(it, UserTable) }
        }
    }

    private fun validateCoupon(code: String, orderAmount: Double, forUpdate: Boolean = false): CouponDAO {
        val query = CouponDAO.find { CouponTable.code eq code and (CouponTable.isActive eq true) }
        val coupon = (if (forUpdate) query.forUpdate() else query).firstOrNull()
            ?: throw ValidationException(Message.Orders.INVALID_COUPON)

        val now = LocalDateTime.now()
        if (now.isBefore(coupon.startDate) || now.isAfter(coupon.endDate))
            throw ValidationException(Message.Orders.COUPON_EXPIRED)

        if (orderAmount < coupon.minOrderAmount)
            throw ValidationException(Message.Orders.couponMinOrderAmount(coupon.minOrderAmount.toString()))

        if (coupon.usageLimit != null && coupon.usageCount >= coupon.usageLimit!!)
            throw ValidationException(Message.Orders.COUPON_LIMIT_REACHED)

        return coupon
    }

    private fun applyCoupon(coupon: CouponDAO, orderAmount: Double): BigDecimal {
        coupon.usageCount += 1
        val discountAmount = when (coupon.discountType) {
            CouponDiscountType.PERCENTAGE -> {
                var amount = BigDecimal(orderAmount).multiply(BigDecimal(coupon.discountValue / 100.0))
                coupon.maxDiscountAmount?.let { amount = amount.min(BigDecimal(it)) }
                amount
            }
            CouponDiscountType.FIXED -> BigDecimal(coupon.discountValue)
        }
        return discountAmount.setScale(2, RoundingMode.HALF_UP)
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
                this.userId = EntityID(userId, UserTable)
                this.shopId = EntityID(shopIdValue, ShopTable)
                this.orderNumber = orderNumber
                this.idempotencyKey = checkoutRequest.idempotencyKey
                this.status = OrderStatus.PENDING
                this.paymentStatus = PaymentStatus.PENDING
                this.subTotal = BigDecimal.ZERO
                this.shippingCost = BigDecimal(shippingMethod.price.toString())
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
                if (product.effectiveStock(forUpdate = true) < cartItem.quantity)
                    throw ValidationException(Message.Validation.insufficientStock(product.name, product.effectiveStock()))

                val unitPrice = product.discountPrice ?: product.price
                val itemTotal = unitPrice.multiply(BigDecimal(cartItem.quantity))

                val orderItem = OrderItemDAO.new {
                    orderId = order.id
                    productId = product.id
                    shopId = EntityID(shopIdValue, ShopTable)
                    quantity = cartItem.quantity
                    price = unitPrice
                    total = itemTotal
                    sku = product.sku
                    productName = product.name
                    taxAmount = BigDecimal.ZERO
                    discountAmount = BigDecimal.ZERO
                }

                product.decrementStock(cartItem.quantity)

                StockReservationDAO.new {
                    this.orderId = order.id
                    this.orderItemId = orderItem.id
                    this.productId = product.id
                    this.shopId = EntityID(shopIdValue, ShopTable)
                    this.quantity = cartItem.quantity
                    this.status = ReservationStatus.ACTIVE
                    this.expiresAt = LocalDateTime.now().plusHours(24)
                }

                shopSubTotal = shopSubTotal.add(itemTotal)
            }

            val taxAmount = shopSubTotal.multiply(BigDecimal(AppConstants.DEFAULT_TAX_PERCENTAGE.toString()))
            order.subTotal = shopSubTotal
            order.taxAmount = taxAmount.setScale(2, RoundingMode.HALF_UP)
            order.total = shopSubTotal.add(order.shippingCost).add(order.taxAmount)
            createdOrders.add(order)
        }

        checkoutRequest.couponCode?.let { code ->
            val totalSubTotal = createdOrders.map { it.subTotal }.reduce(BigDecimal::add)
            val coupon = validateCoupon(code, totalSubTotal.toDouble(), forUpdate = true)
            val discount = applyCoupon(coupon, totalSubTotal.toDouble())
            createdOrders.forEach { order ->
                val proportion = order.subTotal.divide(totalSubTotal, 10, RoundingMode.HALF_UP)
                val orderDiscount = discount.multiply(proportion).setScale(2, RoundingMode.HALF_UP)
                order.discountAmount = orderDiscount
                order.couponCode = code
                order.total = order.total.subtract(orderDiscount)
            }
        }

        val shopIds = createdOrders.mapNotNull { it.shopId?.value }.distinct()
        val sellersByShop = if (shopIds.isNotEmpty()) {
            SellerDAO.find { SellerTable.shopId inList shopIds.map { EntityID(it, ShopTable) } }
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
                    shopId = it.shopId?.value,
                    orderNumber = it.orderNumber,
                    total = it.total.toDouble(),
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

        var subTotal = BigDecimal.ZERO
        var totalItems = 0
        cartItems.forEach { cartItem ->
            val product = productsMap[cartItem.productId.value]!!
            val unitPrice = product.discountPrice ?: product.price
            subTotal = subTotal.add(unitPrice.multiply(BigDecimal(cartItem.quantity)))
            totalItems += cartItem.quantity
        }

        val taxAmount = subTotal.multiply(BigDecimal(AppConstants.DEFAULT_TAX_PERCENTAGE.toString()))
        val baseTotal = subTotal.add(BigDecimal(shippingMethod.price.toString())).add(taxAmount)
        val baseTotalStr = baseTotal.setScale(2, RoundingMode.HALF_UP).toPlainString()
        var response = CheckoutSummaryResponse(
            subTotal = subTotal.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            shippingCost = BigDecimal(shippingMethod.price.toString()).setScale(2, RoundingMode.HALF_UP).toPlainString(),
            taxAmount = taxAmount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            total = baseTotalStr,
            itemCount = totalItems,
        )

        checkoutRequest.couponCode?.let {
            val coupon = validateCoupon(it, subTotal.toDouble())
            val discount = applyCoupon(coupon, subTotal.toDouble())
            val discountedTotal = baseTotal.subtract(discount).setScale(2, RoundingMode.HALF_UP)
            response = response.copy(discountAmount = discount.setScale(2, RoundingMode.HALF_UP).toPlainString(), total = discountedTotal.toPlainString())
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
            if (product.effectiveStock() < item.quantity)
                throw ValidationException(Message.Validation.insufficientStock(product.name, product.effectiveStock()))
            if (product.shopId == null) throw ValidationException(Message.Orders.productDoesNotBelongToShop(product.name))

            val unitPrice = product.discountPrice ?: product.price
            calculatedSubtotal = calculatedSubtotal.add(unitPrice.multiply(BigDecimal(item.quantity)))
        }

        if (BigDecimal(orderRequest.total.toString()).compareTo(calculatedSubtotal) != 0)
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
                this.userId = EntityID(userId, UserTable)
                this.shopId = EntityID(shopIdValue, ShopTable)
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
                val itemTotal = unitPrice.multiply(BigDecimal(itemRequest.quantity))

                OrderItemDAO.new {
                    orderId = order.id
                    productId = product.id
                    shopId = EntityID(shopIdValue, ShopTable)
                    quantity = itemRequest.quantity
                    price = unitPrice
                    total = itemTotal
                    sku = product.sku
                    productName = product.name
                    taxAmount = BigDecimal.ZERO
                    discountAmount = BigDecimal.ZERO
                }

                product.decrementStock(itemRequest.quantity)
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

        order.status = status
        logStatusChange(order.id.value, status, "Status updated by user", userId)
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

        order.status = OrderStatus.CANCELED
        order.canceledDate = LocalDateTime.now()
        order.notes = reason
        logStatusChange(order.id.value, OrderStatus.CANCELED, reason, userId)

        val orderItems = OrderItemDAO.find { OrderItemTable.orderId eq order.id }.toList()
        val productIds = orderItems.map { it.productId.value }
        val productsMap = if (productIds.isNotEmpty()) {
            ProductDAO.find { ProductTable.id inList productIds }.associateBy { it.id.value }
        } else {
            emptyMap()
        }
        orderItems.forEach { orderItem ->
            productsMap[orderItem.productId.value]?.restoreStock(orderItem.quantity)
        }

        StockReservationDAO.find { StockReservationTable.orderId eq order.id }
            .forEach { it.status = ReservationStatus.RELEASED }

        order.toOrderResponse(orderItems.toItemResponses())
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
