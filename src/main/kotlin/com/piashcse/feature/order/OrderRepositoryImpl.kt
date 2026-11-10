package com.piashcse.feature.order

import com.piashcse.constants.*
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.OrderPlacedEvent
import com.piashcse.event.OrderStatusChangedEvent
import com.piashcse.event.OutboxPublisher
import com.piashcse.feature.promotion.PromotionMatch
import com.piashcse.feature.promotion.PromotionService
import com.piashcse.feature.tax.TaxService
import com.piashcse.mapper.toOrderItemResponse
import com.piashcse.mapper.toOrderResponse
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderItemResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.model.response.TaxLineResponse
import com.piashcse.service.CatalogCache
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

class OrderRepositoryImpl(
    private val fxService: FxService = FxService(),
    private val taxService: TaxService = TaxService(),
    private val promotionService: PromotionService = PromotionService(),
) : OrderRepository {
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

        val now = currentUtc()
        if (now.isBefore(coupon.startDate) || now.isAfter(coupon.endDate)) {
            throw ValidationException(Message.Orders.COUPON_EXPIRED)
        }

        if (orderAmount < coupon.minOrderAmount) {
            throw ValidationException(Message.Orders.couponMinOrderAmount(coupon.minOrderAmount.toPlainString()))
        }

        coupon.usageLimit?.let { limit ->
            if (coupon.usageCount >= limit) throw ValidationException(Message.Orders.COUPON_LIMIT_REACHED)
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
                this.usedAt = currentUtc()
            }
        }
    }

    private data class CheckoutContext(
        val cartItems: List<CartItemDAO>,
        val shippingAddress: ShippingAddressDAO,
        val fullAddress: String,
        val shippingMethod: ShippingMethodDAO,
        val productsMap: Map<String, ProductDAO>,
        val variantsMap: Map<String, ProductVariantDAO>,
        val itemsByShop: Map<String, List<CartItemDAO>>,
    )

    private data class CheckoutQuote(
        val orders: List<OrderDAO>,
        val totalUsdSubTotal: BigDecimal,
    )

    override suspend fun placeOrder(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): List<OrderResponse> {
        // Cached once: avoids N+1 UserDAO lookups per split order.
        val callerEmail = query { UserDAO.findById(userId)?.email.orEmpty() }
        val currency = checkoutRequest.currency.uppercase()
        // Fail closed: non-USD checkout requires a stored FX rate (no silent USD-under-foreign-label).
        val fxRate = if (currency != "USD") fxService.rateTo(currency) else BigDecimal.ONE
        val idempotencyKey = checkoutRequest.idempotencyKey
        if (idempotencyKey != null && idempotencyKey.length > 60) {
            throw ValidationException("Idempotency-Key must be at most 60 characters")
        }

        val responses =
            retryQuery {
                existingOrderResponses(userId, idempotencyKey) ?: run {
                    val context = loadCheckoutContext(userId, checkoutRequest)
                    val quote = createOrders(context, userId, checkoutRequest, currency, fxRate)
                    if (checkoutRequest.couponCode != null) {
                        applyCoupon(checkoutRequest.couponCode, quote, context, userId, fxRate)
                    } else {
                        promotionService.evaluateTx(quote.totalUsdSubTotal)?.let { promotion ->
                            applyPromotion(quote, promotion, fxRate)
                            promotionService.markUsedTx(promotion.id)
                        }
                    }
                    updateSellerTotals(quote.orders)
                    context.cartItems.forEach { it.delete() }
                    finalizePlacedOrders(quote.orders, userId, callerEmail)
                    val itemsMap = loadItemsForOrders(quote.orders)
                    quote.orders.map { it.toOrderResponse(itemsMap[it.id.value]) }
                }
            }
        CatalogCache.invalidate()
        return responses
    }

    private fun existingOrderResponses(
        userId: String,
        idempotencyKey: String?,
    ): List<OrderResponse>? {
        if (idempotencyKey == null) return null
        val existing =
            OrderDAO.find {
                (
                    (OrderTable.idempotencyKey eq idempotencyKey) or
                        (OrderTable.idempotencyKey like "$idempotencyKey:%")
                ) and (OrderTable.userId eq userId)
            }.toList()
        if (existing.isEmpty()) return null
        val itemsMap = loadItemsForOrders(existing)
        return existing.map { it.toOrderResponse(itemsMap[it.id.value]) }
    }

    private fun loadCheckoutContext(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutContext {
        val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
        if (cartItems.isEmpty()) throw ValidationException(Message.Cart.EMPTY_CART)

        val shippingAddress =
            ShippingAddressDAO.findById(checkoutRequest.shippingAddressId)
                ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
        if (shippingAddress.userId.value != userId) {
            throw ValidationException(Message.Orders.SHIPPING_ADDRESS_UNAUTHORIZED)
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

        val fullAddress =
            buildString {
                append("${shippingAddress.firstName} ${shippingAddress.lastName}\n")
                append("${shippingAddress.streetAddress}, ${shippingAddress.city}, ${shippingAddress.state ?: ""}\n")
                append("${shippingAddress.country}, ${shippingAddress.zipCode}\n")
                append("Phone: ${maskPhone(shippingAddress.phoneNumber)}")
            }

        return CheckoutContext(
            cartItems = cartItems,
            shippingAddress = shippingAddress,
            fullAddress = fullAddress,
            shippingMethod = shippingMethod,
            productsMap = productsMap,
            variantsMap = variantsMap,
            itemsByShop = itemsByShop,
        )
    }

    private fun createOrders(
        context: CheckoutContext,
        userId: String,
        checkoutRequest: CheckoutRequest,
        currency: String,
        fxRate: BigDecimal,
    ): CheckoutQuote {
        val today = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE)
        var totalUsdSubTotal = BigDecimal.ZERO
        val orders = mutableListOf<OrderDAO>()
        context.itemsByShop.entries.forEachIndexed { index, (shopId, items) ->
            val (order, shopUsdSubTotal) =
                createOrderForShop(
                    context,
                    shopId,
                    items,
                    index,
                    userId,
                    checkoutRequest,
                    currency,
                    fxRate,
                    today,
                )
            totalUsdSubTotal = totalUsdSubTotal.add(shopUsdSubTotal)
            orders.add(order)
        }
        return CheckoutQuote(orders, totalUsdSubTotal)
    }

    private fun createOrderForShop(
        context: CheckoutContext,
        shopIdValue: String,
        items: List<CartItemDAO>,
        index: Int,
        userId: String,
        checkoutRequest: CheckoutRequest,
        currency: String,
        fxRate: BigDecimal,
        today: String,
    ): Pair<OrderDAO, BigDecimal> {
        var shopSubTotal = BigDecimal.ZERO
        var shopUsdSubTotal = BigDecimal.ZERO
        var shopTax = BigDecimal.ZERO
        var shopExclusiveTax = BigDecimal.ZERO
        val businessTaxId = checkoutRequest.businessTaxId

        val order =
            OrderDAO.new {
                this.userId = userId.entityID(UserTable)
                this.shopId = shopIdValue.entityID(ShopTable)
                this.orderNumber = generateOrderNumber(today, index + 1)
                this.idempotencyKey = checkoutRequest.idempotencyKey?.let { "$it:$shopIdValue" }
                this.status = OrderStatus.PENDING
                this.paymentStatus = PaymentStatus.PENDING
                this.subTotal = BigDecimal.ZERO
                this.shippingCost = BigDecimal.ZERO
                this.shippingMethod = context.shippingMethod.name
                this.total = BigDecimal.ZERO
                this.shippingAddress = context.fullAddress
                this.contactEmail = context.shippingAddress.email
                this.paymentMethod = checkoutRequest.paymentMethod
                this.notes = checkoutRequest.notes
                this.currency = currency
                this.fxRate = fxRate
                this.businessTaxId = businessTaxId
                this.taxExempt = taxService.isExempt(userId, businessTaxId, context.shippingAddress.country)
            }

        items.forEach { cartItem ->
            val product =
                context.productsMap[cartItem.productId.value]
                    ?: cartItem.productId.value.throwNotFound("Product")
            val variant = resolveCheckoutVariant(product, cartItem, context.variantsMap)
            val available = product.effectiveStockFor(variant?.id?.value, forUpdate = true)
            if (available < cartItem.quantity) {
                throw ValidationException(Message.Validation.insufficientStock(product.name, available))
            }

            // Product prices are stored in USD; persist the order-currency unit
            // price so refunds and payouts use the same unit as the order.
            val unitPriceUsd = variant?.effectivePrice ?: product.discountPrice ?: product.price
            val unitPrice = Money.scale2(unitPriceUsd.multiply(fxRate))
            val itemTotal = Money.unitTotal(unitPrice, cartItem.quantity)
            shopUsdSubTotal = shopUsdSubTotal.add(Money.unitTotal(unitPriceUsd, cartItem.quantity))
            val taxQuote =
                taxService.resolve(
                    country = context.shippingAddress.country,
                    region = context.shippingAddress.state,
                    categoryId = product.categoryId.value,
                    taxClassId = product.taxClassId?.value,
                    userId = userId,
                    businessTaxId = businessTaxId,
                )
            val taxAmounts = taxService.apply(itemTotal, taxQuote)
            shopTax = shopTax.add(taxAmounts.tax)
            if (!taxQuote.inclusive) shopExclusiveTax = shopExclusiveTax.add(taxAmounts.tax)

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
                    taxAmount = taxAmounts.tax
                    taxRate = taxQuote.rate
                    taxInclusive = taxQuote.inclusive
                    taxClassId = taxQuote.taxClassId?.entityID(TaxClassTable)
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
                this.expiresAt = currentUtc().plusHours(24)
            }

            shopSubTotal = shopSubTotal.add(itemTotal)
        }

        val scaledSubTotal = Money.scale2(shopSubTotal)
        val tax = Money.scale2(shopTax)
        val ship = Money.scale2(context.shippingMethod.price.multiply(fxRate))
        order.subTotal = scaledSubTotal
        order.shippingCost = ship
        order.taxAmount = tax
        order.total = Money.scale2(scaledSubTotal.add(ship).add(shopExclusiveTax))
        return order to shopUsdSubTotal
    }

    private fun resolveCheckoutVariant(
        product: ProductDAO,
        cartItem: CartItemDAO,
        variantsMap: Map<String, ProductVariantDAO>,
    ): ProductVariantDAO? {
        if (product.status != ProductStatus.ACTIVE) throw ValidationException(Message.Products.OUT_OF_STOCK)
        val variant = cartItem.variantId?.value?.let { id -> variantsMap[id] ?: id.throwNotFound("Variant") }
        if (variant != null && variant.productId.value != product.id.value) {
            throw ValidationException("Variant does not belong to product ${product.name}")
        }
        if (variant == null) {
            val requiresVariant =
                ProductVariantDAO.find { ProductVariantTable.productId eq product.id }.firstOrNull() != null
            if (requiresVariant) throw ValidationException("Select a variant for ${product.name}")
        }
        if (variant != null && variant.status != ProductStatus.ACTIVE) {
            throw ValidationException(Message.Products.OUT_OF_STOCK)
        }
        return variant
    }

    private fun applyCoupon(
        code: String,
        quote: CheckoutQuote,
        context: CheckoutContext,
        userId: String,
        fxRate: BigDecimal,
    ) {
        // Coupon thresholds/value are USD-denominated; discounts are converted
        // to the order currency before being applied.
        val coupon =
            validateCoupon(
                code,
                quote.totalUsdSubTotal,
                forUpdate = true,
                userId = userId,
                shopIds = context.itemsByShop.keys,
                categoryIds = context.productsMap.values.map { it.categoryId.value }.toSet(),
            )
        val discountUsd = calculateCouponDiscount(coupon, quote.totalUsdSubTotal)
        consumeCoupon(coupon, userId, quote.orders)
        val totalSubTotal = quote.orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.subTotal) }
        val discount = Money.scale2(discountUsd.multiply(fxRate))
        quote.orders.forEach { order ->
            val orderDiscount = Money.proportionalSplit(discount, order.subTotal, totalSubTotal)
            order.discountAmount = orderDiscount
            order.couponCode = code
            order.total = Money.scale2(order.total.subtract(orderDiscount))
        }
    }

    private fun applyPromotion(
        quote: CheckoutQuote,
        promotion: PromotionMatch,
        fxRate: BigDecimal,
    ) {
        val discount = Money.scale2(promotion.discount.multiply(fxRate))
        val totalSubTotal = quote.orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.subTotal) }
        quote.orders.forEach { order ->
            val orderDiscount =
                if (promotion.freeShipping) {
                    BigDecimal.ZERO.setScale(2)
                } else {
                    Money.proportionalSplit(discount, order.subTotal, totalSubTotal)
                }
            order.discountAmount = orderDiscount
            if (promotion.freeShipping) order.shippingCost = BigDecimal.ZERO.setScale(2)
            order.total =
                Money.scale2(
                    order.subTotal.add(order.shippingCost).add(order.taxAmount).subtract(orderDiscount).max(BigDecimal.ZERO),
                )
        }
    }

    private fun updateSellerTotals(orders: List<OrderDAO>) {
        val shopIds = orders.mapNotNull { it.shopId?.value }.distinct()
        val sellersByShop =
            if (shopIds.isNotEmpty()) {
                SellerDAO.find { SellerTable.shopId inList shopIds.map { it.entityID(ShopTable) } }
                    .associateBy { it.shopId?.value }
            } else {
                emptyMap()
            }
        orders.forEach { order ->
            order.shopId?.value?.let { shopId ->
                sellersByShop[shopId]?.let { seller ->
                    seller.totalSales = seller.totalSales.add(order.subTotal)
                    seller.totalCommission = seller.totalCommission.add(seller.calcCommission(order.subTotal))
                }
            }
        }
    }

    private fun finalizePlacedOrders(
        orders: List<OrderDAO>,
        userId: String,
        callerEmail: String,
    ) {
        orders.forEach {
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
    }

    override suspend fun getCheckoutSummary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutSummaryResponse {
        val currency = checkoutRequest.currency.uppercase()
        val fxRate = if (currency != "USD") fxService.rateTo(currency) else BigDecimal.ONE
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
            val variantIds = cartItems.mapNotNull { it.variantId?.value }.distinct()
            val variantsMap =
                if (variantIds.isNotEmpty()) {
                    ProductVariantDAO.find {
                        ProductVariantTable.id inList variantIds.map { it.entityID(ProductVariantTable) }
                    }.associateBy { it.id.value }
                } else {
                    emptyMap()
                }

            fun unitPriceUsd(cartItem: CartItemDAO): BigDecimal {
                val product =
                    productsMap[cartItem.productId.value]
                        ?: throw ValidationException(Message.Orders.PRODUCT_NOT_FOUND)
                return cartItem.variantId?.value?.let { variantsMap[it] }?.effectivePrice
                    ?: product.discountPrice
                    ?: product.price
            }

            var totalItems = 0
            cartItems.forEach { totalItems += it.quantity }

            val shopCount = cartItems.mapNotNull { productsMap[it.productId.value]?.shopId?.value }.distinct().size

            val subUsd = PricingService.subtotal(cartItems.map { unitPriceUsd(it) to it.quantity })
            val sub = Money.scale2(subUsd.multiply(fxRate))
            val ship = Money.scale2(PricingService.shipping(shippingMethod.price, shopCount).multiply(fxRate))
            var tax = BigDecimal.ZERO
            var exclusiveTax = BigDecimal.ZERO
            val taxLines = mutableListOf<TaxLineResponse>()
            cartItems.forEach { cartItem ->
                val product =
                    productsMap[cartItem.productId.value]
                        ?: throw ValidationException(Message.Orders.PRODUCT_NOT_FOUND)
                val line = Money.scale2(Money.unitTotal(unitPriceUsd(cartItem), cartItem.quantity).multiply(fxRate))
                val quote =
                    taxService.resolve(
                        country = shippingAddress.country,
                        region = shippingAddress.state,
                        categoryId = product.categoryId.value,
                        taxClassId = product.taxClassId?.value,
                        userId = userId,
                        businessTaxId = checkoutRequest.businessTaxId,
                    )
                val amounts = taxService.apply(line, quote)
                tax = tax.add(amounts.tax)
                if (!quote.inclusive) exclusiveTax = exclusiveTax.add(amounts.tax)
                taxLines +=
                    TaxLineResponse(
                        productId = product.id.value,
                        taxClassId = quote.taxClassId,
                        rate = quote.rate.toPlainString(),
                        inclusive = quote.inclusive,
                        netAmount = amounts.net.toPlainString(),
                        taxAmount = amounts.tax.toPlainString(),
                        grossAmount = amounts.gross.toPlainString(),
                    )
            }
            tax = Money.scale2(tax)
            exclusiveTax = Money.scale2(exclusiveTax)

            var response =
                CheckoutSummaryResponse(
                    subTotal = sub.toPlainString(),
                    shippingCost = ship.toPlainString(),
                    taxAmount = tax.toPlainString(),
                    total = Money.scale2(sub.add(ship).add(exclusiveTax)).toPlainString(),
                    itemCount = totalItems,
                    currency = currency,
                    taxLines = taxLines,
                )

            if (checkoutRequest.couponCode == null) {
                promotionService.evaluateTx(subUsd)?.let { promotion ->
                    val discount = Money.scale2(promotion.discount.multiply(fxRate))
                    val shipping = if (promotion.freeShipping) BigDecimal.ZERO.setScale(2) else ship
                    response =
                        response.copy(
                            shippingCost = shipping.toPlainString(),
                            discountAmount = discount.toPlainString(),
                            total =
                                Money.scale2(
                                    sub.add(shipping).add(exclusiveTax).subtract(discount).max(BigDecimal.ZERO),
                                ).toPlainString(),
                        )
                }
            }

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
                val total = Money.scale2(sub.add(ship).add(exclusiveTax).subtract(discount).max(BigDecimal.ZERO))
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

                val fromStatus = order.status
                if (status == OrderStatus.CANCELED) {
                    applyOrderCancellation(order, notes = "Status updated by user", changedBy = userId)
                } else {
                    order.status = status
                    when (status) {
                        OrderStatus.SHIPPED -> {
                            order.shippingDate = currentUtc()
                        }
                        OrderStatus.DELIVERED -> {
                            order.deliveredDate = currentUtc()
                            if (order.shippingDate == null) order.shippingDate = order.deliveredDate
                        }
                        OrderStatus.RECEIVED -> order.completedDate = currentUtc()
                        OrderStatus.PAID -> order.paymentStatus = PaymentStatus.COMPLETED
                        else -> {}
                    }
                    logStatusChange(order.id.value, status, "Status updated by user", userId)
                }
                if (status in setOf(OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.RECEIVED, OrderStatus.CANCELED)) {
                    enqueueStatusChanged(order, fromStatus, status, order.trackingNumber)
                }
                order.toOrderResponse(OrderItemDAO.itemsForOrder(order.id).toItemResponses())
            }
        CatalogCache.invalidate()
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

                val fromStatus = order.status
                applyOrderCancellation(order, notes = reason, changedBy = userId)
                enqueueStatusChanged(order, fromStatus, OrderStatus.CANCELED)
                order.toOrderResponse(OrderItemDAO.itemsForOrder(order.id).toItemResponses())
            }
        CatalogCache.invalidate()
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
                val fromStatus = order.status
                order.status = OrderStatus.SHIPPED
                order.shippingDate = currentUtc()
                order.trackingNumber = trackingNumber
                logStatusChange(order.id.value, OrderStatus.SHIPPED, "Shipped via $trackingNumber", userId)
                enqueueStatusChanged(order, fromStatus, OrderStatus.SHIPPED, trackingNumber)
                order.toOrderResponse(OrderItemDAO.itemsForOrder(order.id).toItemResponses())
            }
        CatalogCache.invalidate()
        return result
    }

    private fun applyOrderCancellation(
        order: OrderDAO,
        notes: String?,
        changedBy: String?,
    ) = OrderCancellation.cancel(order, notes, changedBy)

    private fun enqueueStatusChanged(
        order: OrderDAO,
        from: OrderStatus,
        to: OrderStatus,
        trackingNumber: String? = null,
    ) {
        if (from == to) return
        OutboxPublisher.enqueueTx(
            "ORDER",
            order.id.value,
            OrderStatusChangedEvent(
                orderId = order.id.value,
                userId = order.userId.value,
                email = UserDAO.findById(order.userId.value)?.email.orEmpty(),
                orderNumber = order.orderNumber,
                fromStatus = from.name,
                toStatus = to.name,
                trackingNumber = trackingNumber,
            ),
        )
    }

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
