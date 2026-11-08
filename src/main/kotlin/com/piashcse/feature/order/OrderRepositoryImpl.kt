package com.piashcse.feature.order

import com.piashcse.constants.*
import com.piashcse.database.entities.*
import com.piashcse.event.OrderPlacedEvent
import com.piashcse.event.OutboxPublisher
import com.piashcse.feature.tax.TaxService
import com.piashcse.mapper.toOrderItemResponse
import com.piashcse.mapper.toOrderResponse
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderItemResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.service.CacheService
import com.piashcse.service.FxService
import com.piashcse.service.PricingService
import com.piashcse.utils.common.CursorPage
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.common.cursorPage
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
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
        val shops =
            if (shopIds.isNotEmpty()) {
                ShopDAO.find { ShopTable.id inList shopIds.map { it.entityID(ShopTable) } }.associateBy { it.id.value }
            } else {
                emptyMap()
            }
        shopIds.forEach { shopId ->
            val shop = shops[shopId] ?: throw ValidationException(Message.Orders.SHOP_NOT_FOUND)
            if (shop.status != ShopStatus.APPROVED) {
                throw ValidationException(Message.Orders.SHOP_INACTIVE)
            }
        }
    }

    private fun generateOrderNumber(
        datePrefix: String,
        sequenceNumber: Int,
    ) = "ORD-$datePrefix-${sequenceNumber.toString().padStart(4, '0')}-${UUID.randomUUID().toString().take(8).uppercase()}"

    private fun logStatusChange(
        orderId: String,
        status: OrderStatus,
        notes: String?,
        userId: String?,
    ) {
        OrderStatusHistoryDAO.new {
            this.orderId = orderId.entityID(OrderTable)
            this.status = status
            this.notes = notes
            this.changedBy = userId?.let { it.entityID(UserTable) }
        }
    }

    // PII minimization: never persist the full phone number in the denormalized
    // shippingAddress snapshot — keep only the last 4 digits for support lookup.
    private fun maskPhone(phoneNumber: String): String {
        val digits = phoneNumber.filter { it.isDigit() }
        if (digits.length <= 4) return "***"
        return "***" + digits.takeLast(4)
    }

    private fun validateCoupon(
        code: String,
        orderAmount: BigDecimal,
        forUpdate: Boolean = false,
        userId: String? = null,
        shopIds: Set<String> = emptySet(),
        categoryIds: Set<String> = emptySet(),
    ): CouponDAO {
        val normalizedCode = code.trim().uppercase()
        val query = CouponDAO.find { CouponTable.code eq normalizedCode and (CouponTable.isActive eq true) }
        val coupon =
            (if (forUpdate) query.forUpdate() else query).firstOrNull()
                ?: throw ValidationException(Message.Orders.INVALID_COUPON)

        val now = LocalDateTime.now(ZoneOffset.UTC)
        if (now.isBefore(coupon.startDate) || now.isAfter(coupon.endDate)) {
            throw ValidationException(Message.Orders.COUPON_EXPIRED)
        }

        if (orderAmount < coupon.minOrderAmount) {
            throw ValidationException(Message.Orders.couponMinOrderAmount(coupon.minOrderAmount.toPlainString()))
        }

        if (coupon.usageLimit != null && coupon.usageCount >= coupon.usageLimit!!) {
            throw ValidationException(Message.Orders.COUPON_LIMIT_REACHED)
        }

        // Per-user limit (default 1) is enforced against the usage ledger.
        if (userId != null) {
            val usedByUser =
                CouponUsageDAO.find {
                    (CouponUsageTable.couponId eq coupon.id) and (CouponUsageTable.userId eq userId.entityID(UserTable))
                }.count()
            if (usedByUser >= coupon.perUserLimit) throw ValidationException(Message.Orders.COUPON_LIMIT_REACHED)
        }

        coupon.shopId?.value?.let { couponShop ->
            if (shopIds.isNotEmpty() && couponShop !in shopIds) {
                throw ValidationException("Coupon is not valid for the products in this order")
            }
        }
        coupon.categoryId?.value?.let { couponCategory ->
            if (categoryIds.isNotEmpty() && couponCategory !in categoryIds) {
                throw ValidationException("Coupon is not valid for the product categories in this order")
            }
        }

        return coupon
    }

    private fun calculateCouponDiscount(
        coupon: CouponDAO,
        orderAmount: BigDecimal,
    ): BigDecimal {
        val discount =
            when (coupon.discountType) {
                CouponDiscountType.PERCENTAGE -> Money.percentOf(orderAmount, coupon.discountValue)
                CouponDiscountType.FIXED -> Money.scale2(coupon.discountValue)
            }.let { raw -> coupon.maxDiscountAmount?.let { raw.min(it) } ?: raw }
        return Money.scale2(discount.min(orderAmount).max(BigDecimal.ZERO))
    }

    private fun consumeCoupon(
        coupon: CouponDAO,
        userId: String,
        orders: List<OrderDAO>,
    ) {
        // One usage row per split order and one usageCount increment per row keeps
        // usageCount == ledger size, so limits and reversal stay consistent.
        coupon.usageCount += orders.size
        orders.forEach { order ->
            CouponUsageDAO.new {
                this.couponId = coupon.id
                this.userId = userId.entityID(UserTable)
                this.orderId = order.id
                this.usedAt = LocalDateTime.now(ZoneOffset.UTC)
            }
        }
    }

    override suspend fun placeOrder(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): List<OrderResponse> {
        // Cached once: avoids N+1 UserDAO lookups per split order.
        val callerEmail = query { UserDAO.findById(userId)?.email.orEmpty() }
        val currency = checkoutRequest.currency.uppercase()
        // Fail closed: non-USD checkout requires a stored FX rate (no silent USD-under-foreign-label).
        val fxRate = if (currency != "USD") FxService.rateTo(currency) else BigDecimal.ONE

        checkoutRequest.idempotencyKey?.let { key ->
            if (key.length > 60) {
                throw ValidationException("Idempotency-Key must be at most 60 characters")
            }
        }

        val responses =
            retryQuery {
                val result: List<OrderResponse> =
                    run {
                        checkoutRequest.idempotencyKey?.let { key ->
                            val existing =
                                OrderDAO.find {
                                    ((OrderTable.idempotencyKey eq key) or (OrderTable.idempotencyKey like "$key:%")) and
                                        (OrderTable.userId eq userId)
                                }.toList()
                            if (existing.isNotEmpty()) {
                                val itemsMap = loadItemsForOrders(existing)
                                return@run existing.map { it.toOrderResponse(itemsMap[it.id.value]) }
                            }
                        }

                        val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
                        if (cartItems.isEmpty()) throw ValidationException(Message.Cart.EMPTY_CART)

                        val shippingAddress =
                            ShippingAddressDAO.findById(checkoutRequest.shippingAddressId)
                                ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
                        if (shippingAddress.userId.value != userId) throw ValidationException(Message.Orders.SHIPPING_ADDRESS_UNAUTHORIZED)

                        val fullAddress =
                            buildString {
                                append("${shippingAddress.firstName} ${shippingAddress.lastName}\n")
                                append("${shippingAddress.streetAddress}, ${shippingAddress.city}, ${shippingAddress.state ?: ""}\n")
                                append("${shippingAddress.country}, ${shippingAddress.zipCode}\n")
                                append("Phone: ${maskPhone(shippingAddress.phoneNumber)}")
                            }

                        val shippingMethod =
                            ShippingMethodDAO.findById(checkoutRequest.shippingMethodId)
                                ?: throw ValidationException(Message.Orders.SHIPPING_METHOD_NOT_FOUND)

                        val productsMap =
                            ProductDAO.find {
                                ProductTable.id inList cartItems.map { it.productId.value }.distinct()
                            }.associateBy { it.id.value }
                        val variantIds = cartItems.mapNotNull { it.variantId?.value }.distinct()
                        val variantsMap =
                            if (variantIds.isNotEmpty()) {
                                ProductVariantDAO.find {
                                    ProductVariantTable.id inList variantIds.map { it.entityID(ProductVariantTable) }
                                }.associateBy { it.id.value }
                            } else {
                                emptyMap()
                            }

                        val itemsByShop =
                            cartItems.groupBy {
                                productsMap[it.productId.value]?.shopId?.value
                                    ?: throw ValidationException(Message.Orders.productDoesNotBelongToShop(it.productId.value))
                            }
                        validateShopsApproved(itemsByShop.keys)

                        val createdOrders = mutableListOf<OrderDAO>()
                        val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                        var totalUsdSubTotal = BigDecimal.ZERO

                        itemsByShop.entries.forEachIndexed { index, (shopIdValue, items) ->
                            val orderNumber = generateOrderNumber(today, index + 1)
                            var shopSubTotal = BigDecimal.ZERO
                            var shopTax = BigDecimal.ZERO

                            val order =
                                OrderDAO.new {
                                    this.userId = userId.entityID(UserTable)
                                    this.shopId = shopIdValue.entityID(ShopTable)
                                    this.orderNumber = orderNumber
                                    this.idempotencyKey = checkoutRequest.idempotencyKey?.let { "$it:$shopIdValue" }
                                    this.status = OrderStatus.PENDING
                                    this.paymentStatus = PaymentStatus.PENDING
                                    this.subTotal = BigDecimal.ZERO
                                    this.shippingCost = BigDecimal.ZERO
                                    this.shippingMethod = shippingMethod.name
                                    this.total = BigDecimal.ZERO
                                    this.shippingAddress = fullAddress
                                    this.contactEmail = shippingAddress.email
                                    this.paymentMethod = checkoutRequest.paymentMethod
                                    this.notes = checkoutRequest.notes
                                    this.currency = currency
                                    this.fxRate = fxRate
                                }

                            items.forEach { cartItem ->
                                val product = productsMap[cartItem.productId.value] ?: cartItem.productId.value.throwNotFound("Product")
                                if (product.status != ProductStatus.ACTIVE) {
                                    throw ValidationException(Message.Products.OUT_OF_STOCK)
                                }
                                val variant =
                                    cartItem.variantId?.value?.let { variantId ->
                                        variantsMap[variantId] ?: variantId.throwNotFound("Variant")
                                    }
                                if (variant != null && variant.productId.value != product.id.value) {
                                    throw ValidationException("Variant does not belong to product ${product.name}")
                                }
                                if (variant == null) {
                                    val requiresVariant =
                                        ProductVariantDAO.find { ProductVariantTable.productId eq product.id }
                                            .firstOrNull() != null
                                    if (requiresVariant) {
                                        throw ValidationException("Select a variant for ${product.name}")
                                    }
                                }
                                if (variant != null && variant.status != ProductStatus.ACTIVE) {
                                    throw ValidationException(Message.Products.OUT_OF_STOCK)
                                }
                                val available = product.effectiveStockFor(variant?.id?.value, forUpdate = true)
                                if (available < cartItem.quantity) {
                                    throw ValidationException(Message.Validation.insufficientStock(product.name, available))
                                }

                                // Product prices are stored in USD; persist the order-currency
                                // unit price so refunds and payouts use the same unit as the order.
                                val unitPriceUsd = variant?.effectivePrice ?: product.discountPrice ?: product.price
                                val unitPrice = Money.scale2(unitPriceUsd.multiply(fxRate))
                                val itemTotal = Money.unitTotal(unitPrice, cartItem.quantity)
                                totalUsdSubTotal = totalUsdSubTotal.add(Money.unitTotal(unitPriceUsd, cartItem.quantity))
                                shopTax =
                                    shopTax.add(
                                        Money.tax(
                                            itemTotal,
                                            TaxService.resolve(shippingAddress.country, product.categoryId.value),
                                        ),
                                    )

                                val orderItem =
                                    OrderItemDAO.new {
                                        orderId = order.id
                                        productId = product.id
                                        variantId = variant?.id
                                        shopId = shopIdValue.entityID(ShopTable)
                                        quantity = cartItem.quantity
                                        price = unitPrice
                                        total = itemTotal
                                        sku = variant?.sku ?: product.sku
                                        productName = product.name
                                        variantName = variant?.name
                                        taxAmount = BigDecimal.ZERO
                                        discountAmount = BigDecimal.ZERO
                                    }

                                if (variant == null) {
                                    product.decrementStock(cartItem.quantity)
                                } else {
                                    product.decrementStockFor(variant.id.value, cartItem.quantity)
                                }
                                product.addSales(cartItem.quantity)

                                StockReservationDAO.new {
                                    this.orderId = order.id
                                    this.orderItemId = orderItem.id
                                    this.productId = product.id
                                    this.variantId = variant?.id
                                    this.shopId = shopIdValue.entityID(ShopTable)
                                    this.quantity = cartItem.quantity
                                    this.status = ReservationStatus.ACTIVE
                                    this.expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(24)
                                }

                                shopSubTotal = shopSubTotal.add(itemTotal)
                            }

                            val scaledSubTotal = Money.scale2(shopSubTotal)
                            val tax = Money.scale2(shopTax)
                            val ship = Money.scale2(shippingMethod.price.multiply(fxRate))
                            order.subTotal = scaledSubTotal
                            order.shippingCost = ship
                            order.taxAmount = tax
                            order.total = Money.scale2(scaledSubTotal.add(ship).add(tax))
                            createdOrders.add(order)
                        }

                        checkoutRequest.couponCode?.let { code ->
                            // Coupon thresholds/value are USD-denominated; discounts are
                            // converted to the order currency before being applied.
                            val coupon =
                                validateCoupon(
                                    code,
                                    totalUsdSubTotal,
                                    forUpdate = true,
                                    userId = userId,
                                    shopIds = itemsByShop.keys,
                                    categoryIds = productsMap.values.map { it.categoryId.value }.toSet(),
                                )
                            val discountUsd = calculateCouponDiscount(coupon, totalUsdSubTotal)
                            consumeCoupon(coupon, userId, createdOrders)
                            val totalSubTotal = createdOrders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.subTotal) }
                            val discount = Money.scale2(discountUsd.multiply(fxRate))
                            createdOrders.forEach { order ->
                                val orderDiscount =
                                    Money.proportionalSplit(
                                        discount,
                                        order.subTotal,
                                        totalSubTotal,
                                    )
                                order.discountAmount = orderDiscount
                                order.couponCode = code
                                order.total = Money.scale2(order.total.subtract(orderDiscount))
                            }
                        }

                        val shopIds = createdOrders.mapNotNull { it.shopId?.value }.distinct()
                        val sellersByShop =
                            if (shopIds.isNotEmpty()) {
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
                            // Durable: outbox row commits atomically; OutboxPoller relays (no ghost on rollback/retry).
                            OutboxPublisher.enqueueTx(
                                "ORDER",
                                it.id.value,
                                OrderPlacedEvent(
                                    orderId = it.id.value,
                                    userId = userId,
                                    email = callerEmail,
                                    shopId = it.shopId?.value,
                                    orderNumber = it.orderNumber,
                                    total = it.total,
                                ),
                            )
                        }
                        val itemsMap = loadItemsForOrders(createdOrders)
                        createdOrders.map { it.toOrderResponse(itemsMap[it.id.value]) }
                    }
                result
            }
        CacheService.cache.invalidatePattern("products:.*")
        return responses
    }

    override suspend fun getCheckoutSummary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutSummaryResponse {
        val currency = checkoutRequest.currency.uppercase()
        val fxRate = if (currency != "USD") FxService.rateTo(currency) else BigDecimal.ONE
        return query {
            val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
            if (cartItems.isEmpty()) throw ValidationException(Message.Cart.EMPTY_CART)

            val shippingMethod =
                ShippingMethodDAO.findById(checkoutRequest.shippingMethodId)
                    ?: throw ValidationException(Message.Orders.SHIPPING_METHOD_NOT_FOUND)
            val shippingAddress =
                ShippingAddressDAO.findById(checkoutRequest.shippingAddressId)
                    ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
            if (shippingAddress.userId.value != userId) {
                throw ValidationException(Message.Orders.SHIPPING_ADDRESS_UNAUTHORIZED)
            }

            val productsMap =
                ProductDAO.find {
                    ProductTable.id inList cartItems.map { it.productId.value }.distinct()
                }.associateBy { it.id.value }

            var totalItems = 0
            cartItems.forEach { totalItems += it.quantity }

            val shopCount = cartItems.mapNotNull { productsMap[it.productId.value]?.shopId?.value }.distinct().size

            val subUsd =
                PricingService.subtotal(
                    cartItems.map { cartItem ->
                        val product =
                            productsMap[cartItem.productId.value]
                                ?: throw ValidationException(Message.Orders.PRODUCT_NOT_FOUND)
                        (product.discountPrice ?: product.price) to cartItem.quantity
                    },
                )
            val sub = Money.scale2(subUsd.multiply(fxRate))
            val ship = Money.scale2(PricingService.shipping(shippingMethod.price, shopCount).multiply(fxRate))
            val taxUsd =
                cartItems.fold(BigDecimal.ZERO) { acc, cartItem ->
                    val product =
                        productsMap[cartItem.productId.value]
                            ?: throw ValidationException(Message.Orders.PRODUCT_NOT_FOUND)
                    acc.add(
                        Money.tax(
                            Money.unitTotal(product.discountPrice ?: product.price, cartItem.quantity),
                            TaxService.resolve(shippingAddress.country, product.categoryId.value),
                        ),
                    )
                }
            val tax = Money.scale2(taxUsd.multiply(fxRate))

            var response =
                CheckoutSummaryResponse(
                    subTotal = sub.toPlainString(),
                    shippingCost = ship.toPlainString(),
                    taxAmount = tax.toPlainString(),
                    total = Money.scale2(sub.add(ship).add(tax)).toPlainString(),
                    itemCount = totalItems,
                    currency = currency,
                )

            checkoutRequest.couponCode?.let {
                val coupon =
                    validateCoupon(
                        it,
                        subUsd,
                        userId = userId,
                        shopIds = productsMap.values.mapNotNull { product -> product.shopId?.value }.toSet(),
                        categoryIds = productsMap.values.map { product -> product.categoryId.value }.toSet(),
                    )
                val discountUsd = calculateCouponDiscount(coupon, subUsd)
                val discount = Money.scale2(discountUsd.multiply(fxRate))
                val total = Money.scale2(sub.add(ship).add(tax).subtract(discount).max(BigDecimal.ZERO))
                response =
                    response.copy(
                        discountAmount = discount.toPlainString(),
                        total = total.toPlainString(),
                    )
            }
            response
        }
    }

    override suspend fun getOrders(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> =
        query {
            OrderTable.selectAll().andWhere { OrderTable.userId eq userId }
                .orderBy(OrderTable.createdAt to SortOrder.DESC)
                .toOrdersPaginated(limit, offset)
        }

    override suspend fun getOrdersCursor(
        userId: String,
        limit: Int,
        cursor: String?,
    ): CursorPage<OrderResponse> =
        query {
            val page =
                OrderTable.selectAll()
                    .andWhere { OrderTable.userId eq userId }
                    .cursorPage(limit, cursor, OrderTable.createdAt, OrderTable.id) { it }
            val orders = page.data.map { OrderDAO.wrapRow(it) }
            val itemsMap = loadItemsForOrders(orders)
            CursorPage(
                data = orders.map { it.toOrderResponse(itemsMap[it.id.value]) },
                nextCursor = page.nextCursor,
                hasMore = page.hasMore,
                limit = page.limit,
            )
        }

    override suspend fun getOrderForGuest(
        orderNumber: String,
        email: String,
    ): OrderResponse? =
        query {
            val order =
                OrderDAO.find {
                    (OrderTable.orderNumber eq orderNumber) and
                        (OrderTable.contactEmail eq email.trim().lowercase())
                }.firstOrNull() ?: return@query null
            order.toOrderResponse(OrderItemDAO.itemsForOrder(order.id).toItemResponses())
        }

    override suspend fun updateOrderStatus(
        userId: String,
        orderId: String,
        status: OrderStatus,
    ): OrderResponse {
        val result =
            query {
                userId.requireNotBlank("User ID")
                orderId.requireNotBlank("Order ID")

                val order = OrderDAO.findById(orderId) ?: throw ValidationException(Message.Orders.NOT_FOUND)
                val user = UserDAO.findById(userId) ?: throw ValidationException(Message.Errors.NOT_FOUND)

                requireOrderAccess(order.userId.value, order.shopId?.value, userId, user.userType)

                if (!OrderStatus.canTransitionTo(order.status, status)) {
                    throw ValidationException(Message.Orders.INVALID_STATUS)
                }

                if (status == OrderStatus.CANCELED) {
                    applyOrderCancellation(order, notes = "Status updated by user", changedBy = userId)
                } else {
                    order.status = status
                    when (status) {
                        OrderStatus.SHIPPED -> {
                            order.shippingDate = LocalDateTime.now(ZoneOffset.UTC)
                        }
                        OrderStatus.DELIVERED -> {
                            order.deliveredDate = LocalDateTime.now(ZoneOffset.UTC)
                            if (order.shippingDate == null) order.shippingDate = order.deliveredDate
                        }
                        OrderStatus.RECEIVED -> order.completedDate = LocalDateTime.now(ZoneOffset.UTC)
                        OrderStatus.PAID -> order.paymentStatus = PaymentStatus.COMPLETED
                        else -> {}
                    }
                    logStatusChange(order.id.value, status, "Status updated by user", userId)
                }
                order.toOrderResponse(OrderItemDAO.itemsForOrder(order.id).toItemResponses())
            }
        CacheService.cache.invalidatePattern("products:.*")
        return result
    }

    override suspend fun cancelOrder(
        orderId: String,
        userId: String,
        reason: String,
        userType: UserType,
    ): OrderResponse {
        val result =
            retryQuery {
                orderId.requireNotBlank("Order ID")
                if (reason.isBlank()) throw ValidationException(Message.Orders.CANCEL_REASON_REQUIRED)

                val order = OrderDAO.findById(orderId) ?: throw ValidationException(Message.Orders.NOT_FOUND)

                requireOrderAccess(order.userId.value, order.shopId?.value, userId, userType)
                if (!OrderStatus.canBeCanceled(order.status)) throw ValidationException(Message.Orders.CANNOT_CANCEL)

                applyOrderCancellation(order, notes = reason, changedBy = userId)
                order.toOrderResponse(OrderItemDAO.itemsForOrder(order.id).toItemResponses())
            }
        CacheService.cache.invalidatePattern("products:.*")
        return result
    }

    override suspend fun shipOrder(
        orderId: String,
        userId: String,
        trackingNumber: String,
    ): OrderResponse {
        trackingNumber.requireNotBlank("Tracking number")
        val result =
            query {
                val order = OrderDAO.findById(orderId) ?: throw ValidationException(Message.Orders.NOT_FOUND)
                val user = UserDAO.findById(userId) ?: throw ValidationException(Message.Errors.NOT_FOUND)
                requireOrderAccess(order.userId.value, order.shopId?.value, userId, user.userType)
                if (!OrderStatus.canTransitionTo(order.status, OrderStatus.SHIPPED)) {
                    throw ValidationException(Message.Orders.INVALID_STATUS)
                }
                order.status = OrderStatus.SHIPPED
                order.shippingDate = LocalDateTime.now(ZoneOffset.UTC)
                order.trackingNumber = trackingNumber
                logStatusChange(order.id.value, OrderStatus.SHIPPED, "Shipped via $trackingNumber", userId)
                order.toOrderResponse(OrderItemDAO.itemsForOrder(order.id).toItemResponses())
            }
        CacheService.cache.invalidatePattern("products:.*")
        return result
    }

    private fun applyOrderCancellation(
        order: OrderDAO,
        notes: String?,
        changedBy: String?,
    ) = OrderCancellation.cancel(order, notes, changedBy)

    override suspend fun getSellerOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
    ): PaginatedResponse<OrderResponse> =
        query {
            val seller =
                try {
                    requireSellerByUserId(userId)
                } catch (_: ForbiddenException) {
                    throw ValidationException(Message.Orders.SELLER_PROFILE_NOT_FOUND)
                }
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
    ): PaginatedResponse<OrderResponse> =
        query {
            val query = OrderTable.selectAll()
            status?.let { query.andWhere { OrderTable.status eq OrderStatus.valueOf(it.uppercase()) } }
            startDate?.let { query.andWhere { OrderTable.createdAt greaterEq LocalDateTime.ofInstant(it, ZoneOffset.UTC) } }
            endDate?.let { query.andWhere { OrderTable.createdAt lessEq LocalDateTime.ofInstant(it, ZoneOffset.UTC) } }
            query.orderBy(OrderTable.createdAt to SortOrder.DESC).toOrdersPaginated(limit, offset)
        }
}
