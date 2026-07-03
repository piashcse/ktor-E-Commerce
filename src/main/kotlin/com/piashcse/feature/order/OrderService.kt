package com.piashcse.feature.order

import com.piashcse.constants.AppConstants
import com.piashcse.constants.CouponDiscountType
import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.mapper.toOrderItemResponse
import com.piashcse.mapper.toOrderResponse
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.OrderRequest
import com.piashcse.model.response.CheckoutSummaryResponse
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
import java.util.UUID

class OrderService : OrderRepository {

    override suspend fun placeOrder(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): List<OrderResponse> = query {
        checkoutRequest.idempotencyKey?.let { key ->
            OrderDAO.find { OrderTable.idempotencyKey eq key }.toList().takeIf { it.isNotEmpty() }
                ?.let { return@query it.map { it.toOrderResponse() } }
        }

        val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
        if (cartItems.isEmpty()) throw ValidationException(Message.Cart.EMPTY_CART)

        val shippingAddress = ShippingAddressDAO.findById(checkoutRequest.shippingAddressId)
            ?: throw ValidationException("Shipping address not found")
        if (shippingAddress.userId.value != userId) throw ValidationException("Unauthorized shipping address")

        val fullAddress = buildString {
            append("${shippingAddress.firstName} ${shippingAddress.lastName}\n")
            append("${shippingAddress.streetAddress}, ${shippingAddress.city}, ${shippingAddress.state ?: ""}\n")
            append("${shippingAddress.country}, ${shippingAddress.zipCode}\n")
            append("Phone: ${shippingAddress.phoneNumber}")
        }

        val shippingMethod = ShippingMethodDAO.findById(checkoutRequest.shippingMethodId)
            ?: throw ValidationException("Shipping method not found")

        val productsMap = ProductDAO.find {
            ProductTable.id inList cartItems.map { it.productId.value }.distinct()
        }.associateBy { it.id.value }

        val itemsByShop = cartItems.groupBy {
            productsMap[it.productId.value]?.shopId?.value
                ?: throw ValidationException("Product ${it.productId.value} does not belong to any shop")
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
                val product = productsMap[cartItem.productId.value]!!
                if (product.effectiveStock() < cartItem.quantity)
                    throw ValidationException(Message.Validation.insufficientStock(product.name, product.effectiveStock()))

                val unitPrice = product.discountPrice ?: product.price
                val itemTotal = unitPrice.multiply(BigDecimal(cartItem.quantity))

                OrderItemDAO.new {
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
            val discount = validateAndApplyCoupon(code, totalSubTotal.toDouble())
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
        createdOrders.forEach { logStatusChange(it.id.value, it.status, "Order placed", userId) }
        createdOrders.map { it.toOrderResponse() }
    }

    private fun logStatusChange(orderId: String, status: OrderStatus, notes: String?, userId: String?) {
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
    ): CheckoutSummaryResponse = query {
        val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
        if (cartItems.isEmpty()) throw ValidationException(Message.Cart.EMPTY_CART)

        val shippingMethod = ShippingMethodDAO.findById(checkoutRequest.shippingMethodId)
            ?: throw ValidationException("Shipping method not found")

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
            val discount = validateAndApplyCoupon(it, subTotal.toDouble())
            val discountedTotal = baseTotal.subtract(discount).setScale(2, RoundingMode.HALF_UP)
            response = response.copy(discountAmount = discount.setScale(2, RoundingMode.HALF_UP).toPlainString(), total = discountedTotal.toPlainString())
        }
        response
    }

    private fun validateAndApplyCoupon(code: String, orderAmount: Double): BigDecimal {
        val coupon = CouponDAO.find { CouponTable.code eq code and (CouponTable.isActive eq true) }.firstOrNull()
            ?: throw ValidationException("Invalid or inactive coupon code")

        val now = LocalDateTime.now()
        if (now.isBefore(coupon.startDate) || now.isAfter(coupon.endDate))
            throw ValidationException("Coupon is expired or not yet active")

        if (orderAmount < coupon.minOrderAmount)
            throw ValidationException("Order amount is below the minimum required for this coupon ($${coupon.minOrderAmount})")

        if (coupon.usageLimit != null && coupon.usageCount >= coupon.usageLimit!!)
            throw ValidationException("Coupon usage limit reached")

        val discountAmount = when (coupon.discountType) {
            CouponDiscountType.PERCENTAGE -> {
                var amount = BigDecimal(orderAmount).multiply(BigDecimal(coupon.discountValue / 100.0))
                coupon.maxDiscountAmount?.let { amount = amount.min(BigDecimal(it)) }
                amount
            }
            CouponDiscountType.FIXED -> BigDecimal(coupon.discountValue)
        }
        coupon.usageCount += 1
        return discountAmount.setScale(2, RoundingMode.HALF_UP)
    }

    override suspend fun createOrder(
        userId: String,
        orderRequest: OrderRequest,
        idempotencyKey: String?,
    ): List<OrderResponse> = query {
        userId.requireNotBlank("User ID")
        if (orderRequest.orderItems.isEmpty()) throw ValidationException(Message.Validation.EMPTY_ORDER_ITEMS)

        idempotencyKey?.let { key ->
            OrderDAO.find { OrderTable.idempotencyKey eq key }.firstOrNull()
                ?.let { return@query listOf(it.toOrderResponse()) }
        }

        val productsMap = ProductDAO.find {
            ProductTable.id inList orderRequest.orderItems.map { it.productId }.distinct()
        }.associateBy { it.id.value }

        var calculatedSubtotal = BigDecimal.ZERO
        orderRequest.orderItems.forEach { item ->
            val product = productsMap[item.productId]
                ?: throw ValidationException(Message.Validation.productNotFound(item.productId))
            if (product.effectiveStock() < item.quantity)
                throw ValidationException(Message.Validation.insufficientStock(product.name, product.effectiveStock()))
            if (product.shopId == null) throw ValidationException(Message.Orders.productDoesNotBelongToShop(product.name))

            val unitPrice = product.discountPrice ?: product.price
            calculatedSubtotal = calculatedSubtotal.add(unitPrice.multiply(BigDecimal(item.quantity)))
        }

        if (BigDecimal(orderRequest.total.toString()).compareTo(calculatedSubtotal) != 0)
            throw ValidationException("Order total mismatch. Requested: ${orderRequest.total}, Calculated: $calculatedSubtotal")

        val itemsByShop = orderRequest.orderItems.groupBy { productsMap[it.productId]!!.shopId!!.value }
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

        createdOrders.map { it.toOrderResponse() }
    }

    private fun validateShopsApproved(shopIds: Set<String>) {
        shopIds.forEach { shopId ->
            val shop = ShopDAO.findById(shopId) ?: throw ValidationException("Shop $shopId not found")
            if (shop.status != ShopStatus.APPROVED)
                throw ValidationException("Shop '${shop.name}' is not active. Cannot place order.")
        }
    }

    private fun generateOrderNumber(datePrefix: String, sequenceNumber: Int) =
        "ORD-$datePrefix-${sequenceNumber.toString().padStart(4, '0')}-${UUID.randomUUID().toString().take(8).uppercase()}"

    private fun Query.toOrdersPaginated(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> {
        val (totalCount, rows) = toPaginatedList(limit, offset) { OrderDAO.wrapRow(it) }
        val itemsMap = OrderItemDAO.itemsForOrders(rows.map { it.id })
        val data = rows.map { order -> order.toOrderResponse(itemsMap[order.id.value]?.map { it.toOrderItemResponse() }) }
        return PaginatedResponse(data, PaginationMetadata(totalCount, limit, offset))
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

        order.status = status
        logStatusChange(order.id.value, status, "Status updated by user", userId)
        order.toOrderResponse()
    }

    override suspend fun cancelOrder(
        orderId: String,
        userId: String,
        reason: String,
        userType: UserType,
    ): OrderResponse = query {
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

        OrderItemDAO.find { OrderItemTable.orderId eq order.id }.forEach { orderItem ->
            ProductDAO.findById(orderItem.productId.value)?.restoreStock(orderItem.quantity)
        }

        order.toOrderResponse()
    }

    override suspend fun getSellerOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
    ): PaginatedResponse<OrderResponse> = query {
        val seller = findSellerByUserId(userId) ?: throw ValidationException("Seller profile not found")
        val shopId = seller.shopId ?: throw ValidationException("No shop associated with seller")

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
