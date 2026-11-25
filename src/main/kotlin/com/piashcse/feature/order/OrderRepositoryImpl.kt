package com.piashcse.feature.order

import com.piashcse.constants.*
import com.piashcse.constants.AppConstants
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.OrderPlacedEvent
import com.piashcse.event.OutboxAggregate
import com.piashcse.event.OutboxPublisher
import com.piashcse.feature.config.StorefrontSettings
import com.piashcse.feature.coupon.CouponRules
import com.piashcse.feature.coupon.toSnapshot
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.promotion.PromotionMatch
import com.piashcse.feature.promotion.PromotionService
import com.piashcse.feature.seller.SellerLedger
import com.piashcse.feature.tax.TaxQuote
import com.piashcse.feature.tax.TaxService
import com.piashcse.mapper.toOrderResponse
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.service.CatalogCache
import com.piashcse.service.DigitalDeliveryService
import com.piashcse.service.FxService
import com.piashcse.service.PricingService
import com.piashcse.utils.common.CursorPage
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.db.isUniqueViolation
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class OrderRepositoryImpl(
    private val fxService: FxService,
    private val taxService: TaxService,
    private val promotionService: PromotionService,
    private val loyaltyService: LoyaltyService,
    private val digitalDelivery: DigitalDeliveryService,
) : OrderRepository {
    private val checkoutPreview = CheckoutPreviewService(fxService, taxService, promotionService, loyaltyService)

    private fun generatePickupCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { alphabet[kotlin.random.Random.nextInt(alphabet.length)] }.joinToString("")
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

    private fun consumeCoupon(
        coupon: CouponDAO,
        userId: String,
        orders: List<OrderDAO>,
        checkoutReference: String,
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
                this.checkoutReference = checkoutReference
            }
        }
    }

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
        val fxRate = if (currency != AppConstants.DEFAULT_CURRENCY) fxService.rateTo(currency) else BigDecimal.ONE
        val idempotencyKey = checkoutRequest.idempotencyKey
        if (idempotencyKey != null && idempotencyKey.length > 60) {
            throw ValidationException("Idempotency-Key must be at most 60 characters")
        }

        val responses =
            try {
                retryQuery {
                    existingOrderResponses(userId, idempotencyKey) ?: run {
                        val checkoutReference = UUID.randomUUID().toString()
                        val context = CheckoutContextLoader.load(userId, checkoutRequest)
                        val quote = createOrders(context, userId, checkoutRequest, currency, fxRate)
                        if (checkoutRequest.couponCode != null) {
                            applyCoupon(checkoutRequest.couponCode, quote, context, userId, fxRate, checkoutReference)
                        } else {
                            promotionService.evaluateTx(quote.totalUsdSubTotal, context.itemsByShop.keys)?.let { promotion ->
                                applyPromotion(quote, promotion, fxRate)
                                promotionService.markUsedTx(
                                    promotion.id,
                                    userId,
                                    quote.orders.map { it.id.value },
                                    checkoutReference,
                                )
                            }
                        }
                        recalculateTaxes(quote)
                        applyWalletRedemptions(quote, checkoutRequest, userId, fxRate)
                        createOfflinePayments(quote.orders)
                        updateSellerTotals(quote.orders)
                        context.cartItems.forEach { it.delete() }
                        completeZeroTotalOrders(quote.orders)
                        finalizePlacedOrders(quote.orders, userId, callerEmail)
                        val itemsMap = loadItemsForOrders(quote.orders)
                        quote.orders.map { it.toOrderResponse(itemsMap[it.id.value]) }
                    }
                }
            } catch (error: Exception) {
                if (error.isUniqueViolation() && idempotencyKey != null) {
                    query { existingOrderResponses(userId, idempotencyKey) } ?: throw error
                } else {
                    throw error
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
        val escapedKey = idempotencyKey.escapeLike()
        val existing =
            OrderDAO.find {
                (
                    (OrderTable.idempotencyKey eq idempotencyKey) or
                        (OrderTable.idempotencyKey like "$escapedKey:%")
                ) and (OrderTable.userId eq userId)
            }.toList()
        if (existing.isEmpty()) return null
        val itemsMap = loadItemsForOrders(existing)
        return existing.map { it.toOrderResponse(itemsMap[it.id.value]) }
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
        val cartUsdSubtotal =
            context.cartItems.fold(BigDecimal.ZERO) { acc, cartItem ->
                val product = context.productsMap[cartItem.productId.value] ?: return@fold acc
                val variant = resolveCheckoutVariant(product, cartItem, context.variantsMap)
                acc.add(Money.unitTotal(PricingService.unitPriceUsd(product, variant), cartItem.quantity))
            }
        val freeShippingByThreshold = StorefrontSettings.freeShippingReached(cartUsdSubtotal)
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
                    freeShippingByThreshold,
                )
            totalUsdSubTotal = totalUsdSubTotal.add(shopUsdSubTotal)
            orders.add(order)
        }
        return CheckoutQuote(orders, totalUsdSubTotal)
    }

    private fun createOrderShell(
        context: CheckoutContext,
        shopIdValue: String,
        index: Int,
        userId: String,
        checkoutRequest: CheckoutRequest,
        currency: String,
        fxRate: BigDecimal,
        today: String,
    ): OrderDAO =
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
            this.contactEmail = context.contactEmail
            this.paymentMethod = checkoutRequest.paymentMethod
            this.notes = checkoutRequest.notes
            this.currency = currency
            this.fxRate = fxRate
            this.businessTaxId = checkoutRequest.businessTaxId
            this.taxExempt = taxService.isExempt(userId, checkoutRequest.businessTaxId, context.taxCountry)
            this.locale = com.piashcse.constants.Localization.normalize(checkoutRequest.locale)
            this.fulfillmentType = checkoutRequest.fulfillmentType
            this.pickupWarehouseId = context.pickupWarehouse?.id
            if (context.pickupWarehouse != null) {
                this.shippingMethod = "PICKUP"
                this.pickupCode = generatePickupCode()
                this.pickupStatus = com.piashcse.constants.PickupStatus.PENDING
            }
            this.attributionSessionId = checkoutRequest.attribution?.sessionId
            this.attributionSource = checkoutRequest.attribution?.source
            this.attributionMedium = checkoutRequest.attribution?.medium
            this.attributionCampaign = checkoutRequest.attribution?.campaign
            this.attributionReferrer = checkoutRequest.attribution?.referrer
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
        freeShippingByThreshold: Boolean,
    ): Pair<OrderDAO, BigDecimal> {
        var shopSubTotal = BigDecimal.ZERO
        var shopUsdSubTotal = BigDecimal.ZERO
        var shopTax = BigDecimal.ZERO
        var shopExclusiveTax = BigDecimal.ZERO
        var allDigital = true
        var allFreeShipping = true
        val businessTaxId = checkoutRequest.businessTaxId

        val order = createOrderShell(context, shopIdValue, index, userId, checkoutRequest, currency, fxRate, today)

        items.forEach { cartItem ->
            val product =
                context.productsMap[cartItem.productId.value]
                    ?: cartItem.productId.value.throwNotFound("Product")
            val variant = resolveCheckoutVariant(product, cartItem, context.variantsMap)
            val pickupWarehouseId = context.pickupWarehouse?.id?.value
            if (!product.isDigital) {
                val available =
                    if (pickupWarehouseId != null) {
                        product.effectiveStockForAt(pickupWarehouseId, variant?.id?.value)
                    } else {
                        product.effectiveStockFor(variant?.id?.value, forUpdate = true)
                    }
                if (available < cartItem.quantity) {
                    throw ValidationException(Message.Validation.insufficientStock(product.name, available))
                }
            }

            // Product prices are stored in USD; persist the order-currency unit
            // price so refunds and payouts use the same unit as the order.
            if (!product.isDigital) allDigital = false
            if (!product.isDigital && !product.freeShipping) allFreeShipping = false
            val unitPriceUsd = PricingService.unitPriceUsd(product, variant)
            val unitPrice = PricingService.toCurrency(unitPriceUsd, fxRate)
            val itemTotal = Money.unitTotal(unitPrice, cartItem.quantity)
            shopUsdSubTotal = shopUsdSubTotal.add(Money.unitTotal(unitPriceUsd, cartItem.quantity))
            val taxQuote =
                taxService.resolve(
                    country = context.taxCountry,
                    region = context.taxRegion,
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
                    isDigital = product.isDigital
                    discountAmount = BigDecimal.ZERO
                }

            val allocations =
                if (product.isDigital) {
                    emptyList()
                } else {
                    when {
                        pickupWarehouseId != null ->
                            product.decrementStockAt(pickupWarehouseId, variant?.id?.value, cartItem.quantity)
                        variant == null -> product.decrementStock(cartItem.quantity)
                        else -> product.decrementStockFor(variant.id.value, cartItem.quantity)
                    }
                }
            product.addSales(cartItem.quantity)

            allocations.forEach { allocation ->
                StockReservationDAO.new {
                    this.orderId = order.id
                    this.orderItemId = orderItem.id
                    this.productId = product.id
                    this.variantId = variant?.id
                    this.shopId = shopIdValue.entityID(ShopTable)
                    this.warehouseId = allocation.warehouseId.entityID(WarehouseTable)
                    this.quantity = allocation.quantity
                    this.restoredQuantity = 0
                    this.status = ReservationStatus.ACTIVE
                    // Offline payments (COD/bank transfer) settle after
                    // fulfilment, so they need a much longer reservation window.
                    val offline =
                        checkoutRequest.paymentMethod in
                            listOf(PaymentMethod.COD, PaymentMethod.BANK_TRANSFER)
                    this.expiresAt = currentUtc().plusHours(if (offline) 24L * 30 else 24)
                }
            }

            shopSubTotal = shopSubTotal.add(itemTotal)
        }

        val scaledSubTotal = Money.scale2(shopSubTotal)
        val tax = Money.scale2(shopTax)
        val ship =
            if (allDigital || context.pickupWarehouse != null || allFreeShipping || freeShippingByThreshold) {
                BigDecimal.ZERO.setScale(2)
            } else {
                PricingService.toCurrency(context.shippingMethod.price, fxRate)
            }
        order.subTotal = scaledSubTotal
        order.shippingCost = ship
        if (allDigital) order.shippingMethod = "DIGITAL"
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
        if (cartItem.quantity < product.minOrderQuantity) {
            throw ValidationException("Minimum order quantity for ${product.name} is ${product.minOrderQuantity}")
        }
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
        checkoutReference: String,
    ) {
        // Coupon thresholds/value are USD-denominated; discounts are converted
        // to the order currency before being applied.
        val coupon = CouponCheckoutSupport.load(code, forUpdate = true, userId = userId)
        val snapshot = coupon.toSnapshot()
        val eligibleUsd =
            CouponRules.eligibleSubtotalUsd(
                snapshot,
                CouponCheckoutSupport.lines(context.cartItems, context.productsMap, context.variantsMap),
            )
        CouponRules.eligibilityError(snapshot, eligibleUsd)?.let { throw ValidationException(it) }
        val discountUsd = CouponRules.discountUsd(snapshot, eligibleUsd)
        consumeCoupon(coupon, userId, quote.orders, checkoutReference)
        val eligibleByOrder =
            quote.orders.associate { order ->
                val orderItems = context.itemsByShop[order.shopId?.value].orEmpty()
                order.id.value to
                    CouponRules.eligibleSubtotalUsd(
                        snapshot,
                        CouponCheckoutSupport.lines(orderItems, context.productsMap, context.variantsMap),
                    )
            }
        val totalEligible = eligibleByOrder.values.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
        val discount = PricingService.toCurrency(discountUsd, fxRate)
        quote.orders.forEach { order ->
            val orderEligible = eligibleByOrder[order.id.value] ?: BigDecimal.ZERO
            val orderDiscount =
                if (totalEligible > BigDecimal.ZERO) {
                    Money.proportionalSplit(discount, orderEligible, totalEligible)
                } else {
                    BigDecimal.ZERO.setScale(2)
                }
            order.discountAmount = orderDiscount
            order.couponCode = code
            order.total = Money.scale2(order.total.subtract(orderDiscount))

            // Persist the coupon discount on the lines it actually applies to;
            // tax and seller settlement must not discount ineligible lines.
            val orderItems = OrderItemDAO.itemsForOrder(order.id)
            val eligibleItems =
                orderItems.filter { item ->
                    val product = context.productsMap[item.productId.value] ?: return@filter false
                    CouponRules.matches(snapshot, product.shopId?.value, product.categoryId.value)
                }
            val eligibleTotal = eligibleItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.total) }
            orderItems.forEach { item ->
                item.discountAmount =
                    if (item in eligibleItems && eligibleTotal > BigDecimal.ZERO) {
                        Money.proportionalSplit(orderDiscount, item.total, eligibleTotal)
                    } else {
                        BigDecimal.ZERO.setScale(2)
                    }
            }
        }
    }

    private fun applyPromotion(
        quote: CheckoutQuote,
        promotion: PromotionMatch,
        fxRate: BigDecimal,
    ) {
        val discount = PricingService.toCurrency(promotion.discount, fxRate)
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
            order.total = Money.scale2(order.subTotal.add(order.shippingCost).subtract(orderDiscount).max(BigDecimal.ZERO))
            OrderItemDAO.itemsForOrder(order.id).forEach { item ->
                item.discountAmount =
                    if (order.subTotal > BigDecimal.ZERO) {
                        Money.proportionalSplit(orderDiscount, item.total, order.subTotal)
                    } else {
                        BigDecimal.ZERO.setScale(2)
                    }
            }
        }
    }

    private fun applyWalletRedemptions(
        quote: CheckoutQuote,
        request: CheckoutRequest,
        userId: String,
        fxRate: BigDecimal,
    ) {
        val points = request.usePoints.coerceAtLeast(0)
        val creditUsd = request.walletCredit?.takeIf { it > BigDecimal.ZERO }
        val giftCode = request.giftCardCode?.takeIf { it.isNotBlank() }
        if (points == 0 && creditUsd == null && giftCode == null) return

        val orders = quote.orders
        val total = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.total) }
        if (total <= BigDecimal.ZERO) return
        val totalUsd = total.divide(fxRate, 2, RoundingMode.HALF_UP)
        var remainingUsd = totalUsd
        val balances = loyaltyService.redemptionPreviewTx(userId, points, creditUsd, giftCode)

        // Cap every redemption before debiting it; never burn points/credit that
        // cannot be applied.
        val pointsToRedeem =
            if (points > 0) {
                points
                    .coerceAtMost(balances.pointsAvailable)
                    .coerceAtMost(
                        remainingUsd.multiply(BigDecimal(100)).setScale(0, RoundingMode.DOWN).toInt(),
                    )
            } else {
                0
            }
        val pointsUsd =
            BigDecimal(pointsToRedeem)
                .divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
        remainingUsd = remainingUsd.subtract(pointsUsd).max(BigDecimal.ZERO)
        val creditToRedeem =
            creditUsd
                ?.min(remainingUsd)
                ?.min(balances.creditAvailable)
                ?.max(BigDecimal.ZERO)
                ?: BigDecimal.ZERO
        remainingUsd = remainingUsd.subtract(creditToRedeem).max(BigDecimal.ZERO)
        val giftBudgetUsd = remainingUsd

        // Allocate redemptions per order so cancelling one split order reverses
        // only that order's share instead of the whole checkout.
        var pointsLeft = pointsToRedeem
        var creditLeft = creditToRedeem
        var giftLeft = giftBudgetUsd
        val walletUsdByOrder = mutableMapOf<String, BigDecimal>()
        val giftUsdByOrder = mutableMapOf<String, BigDecimal>()
        orders.forEachIndexed { index, order ->
            val last = index == orders.lastIndex
            val pointsShare =
                if (last) {
                    pointsLeft
                } else if (total > BigDecimal.ZERO) {
                    BigDecimal(pointsToRedeem)
                        .multiply(order.total)
                        .divide(total, 0, RoundingMode.DOWN)
                        .toInt()
                } else {
                    0
                }
            val redeemedPoints =
                if (pointsShare > 0) {
                    loyaltyService.redeemPointsTx(userId, pointsShare, order.id.value)
                } else {
                    BigDecimal.ZERO
                }
            pointsLeft = (pointsLeft - pointsShare).coerceAtLeast(0)

            val creditShare =
                if (last) {
                    creditLeft
                } else {
                    Money.proportionalSplit(creditToRedeem, order.total, total)
                }
            val redeemedCredit =
                if (creditShare > BigDecimal.ZERO) {
                    loyaltyService.redeemCreditTx(userId, creditShare, order.id.value)
                } else {
                    BigDecimal.ZERO
                }
            creditLeft = creditLeft.subtract(redeemedCredit).max(BigDecimal.ZERO)

            val giftShare =
                if (last) {
                    giftLeft
                } else {
                    Money.proportionalSplit(giftBudgetUsd, order.total, total)
                }
            val redeemedGift =
                giftCode?.let {
                    loyaltyService.redeemGiftCardTx(it, userId, order.id.value, giftShare)
                } ?: BigDecimal.ZERO
            giftLeft = giftLeft.subtract(redeemedGift).max(BigDecimal.ZERO)

            walletUsdByOrder[order.id.value] = redeemedPoints.add(redeemedCredit)
            giftUsdByOrder[order.id.value] = redeemedGift
        }

        orders.forEach { order ->
            val walletTotal = walletUsdByOrder[order.id.value].orZero().multiply(fxRate).min(order.total)
            order.walletDiscount = walletTotal
            order.total = Money.scale2(order.total.subtract(walletTotal))
            val giftTotal =
                giftUsdByOrder[order.id.value]
                    .orZero()
                    .multiply(fxRate)
                    .min(order.total.subtract(walletTotal).max(BigDecimal.ZERO))
            order.giftCardDiscount = giftTotal
            order.giftCardCode = giftCode
            order.total = Money.scale2(order.total.subtract(giftTotal))
            order.discountAmount = Money.scale2(order.discountAmount.add(walletTotal).add(giftTotal))
        }
    }

    private fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO

    /**
     * Recomputes line tax on the post-coupon/post-promotion taxable amount.
     * Inclusive lines already carry gross values; exclusive lines add tax on top.
     */
    private fun recalculateTaxes(quote: CheckoutQuote) {
        quote.orders.forEach { order ->
            val items = OrderItemDAO.itemsForOrder(order.id)
            if (items.isEmpty()) return@forEach
            // Wallet credit and gift cards are payment instruments, not price
            // discounts; only coupon/promotion amounts reduce the taxable base.
            val discount =
                order.discountAmount
                    .subtract(order.walletDiscount)
                    .subtract(order.giftCardDiscount)
                    .max(BigDecimal.ZERO)
            var allTax = BigDecimal.ZERO
            var exclusiveTax = BigDecimal.ZERO
            items.forEach { item ->
                // Coupon/promotion allocation was persisted on the line; wallet
                // and gift-card amounts were deliberately excluded above.
                val discountShare = item.discountAmount.max(BigDecimal.ZERO)
                val taxable = item.total.subtract(discountShare).max(BigDecimal.ZERO)
                val amounts =
                    taxService.apply(
                        taxable,
                        com.piashcse.feature.tax.TaxQuote(
                            rate = item.taxRate,
                            inclusive = item.taxInclusive,
                            taxClassId = item.taxClassId?.value,
                        ),
                    )
                item.taxAmount = amounts.tax
                allTax = allTax.add(amounts.tax)
                if (!item.taxInclusive) exclusiveTax = exclusiveTax.add(amounts.tax)
            }
            order.taxAmount = Money.scale2(allTax)
            order.total =
                Money.scale2(
                    order.subTotal.subtract(discount).max(BigDecimal.ZERO)
                        .add(order.shippingCost)
                        .add(exclusiveTax),
                )
        }
    }

    private fun updateSellerTotals(orders: List<OrderDAO>) {
        val itemsByOrder = OrderItemDAO.itemsForOrders(orders.map { it.id })
        SellerLedger.applyOrders(orders, itemsByOrder)
    }

    /**
     * COD and bank-transfer orders need a provider-visible payment row before
     * collection. Without it an admin has nothing to confirm and a seller
     * cannot ship a COD order. The row stays PENDING until an admin confirms
     * the cash/transfer.
     */
    private fun createOfflinePayments(orders: List<OrderDAO>) {
        val payable =
            orders.filter {
                it.total > BigDecimal.ZERO &&
                    it.paymentMethod in listOf(PaymentMethod.COD, PaymentMethod.BANK_TRANSFER)
            }
        if (payable.isEmpty()) return
        val existing =
            PaymentDAO.find {
                (PaymentTable.orderId inList payable.map { it.id }) and
                    (PaymentTable.status eq PaymentStatus.PENDING)
            }.map { it.orderId.value }.toSet()
        payable.filterNot { it.id.value in existing }.forEach { order ->
            val method = order.paymentMethod ?: return@forEach
            PaymentDAO.new {
                this.orderId = order.id
                this.userId = order.userId
                amount = order.total
                currency = order.currency
                status = PaymentStatus.PENDING
                paymentMethod = method
                transactionId = "offline:${order.id.value}"
                provider = PaymentProvider.MANUAL
                providerStatus = "awaiting-${method.name.lowercase()}"
                scaStatus = ScaStatus.NOT_REQUIRED
            }
        }
    }

    /**
     * Wallet credit, gift cards or a 100% coupon can make an order total zero.
     * There is no remaining amount for `POST /payments`, so complete those
     * orders here with the same side effects as a successful payment.
     */
    private fun completeZeroTotalOrders(orders: List<OrderDAO>) {
        orders
            .filter { it.total <= BigDecimal.ZERO && it.paymentStatus != PaymentStatus.COMPLETED }
            .forEach { OrderCompletion.completeZeroTotalTx(it, loyaltyService, digitalDelivery) }
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
                OutboxAggregate.ORDER,
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
    ): CheckoutSummaryResponse = checkoutPreview.summary(userId, checkoutRequest)

    override suspend fun getOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
        query: String?,
        from: Instant?,
        to: Instant?,
    ): PaginatedResponse<OrderResponse> = OrderQueries.getOrders(userId, limit, offset, status, query, from, to)

    override suspend fun getOrdersCursor(
        userId: String,
        limit: Int,
        cursor: String?,
    ): CursorPage<OrderResponse> = OrderQueries.getOrdersCursor(userId, limit, cursor)

    override suspend fun getOrderForGuest(
        orderNumber: String,
        email: String,
    ): OrderResponse? = OrderQueries.getOrderForGuest(orderNumber, email)

    override suspend fun getOrderById(
        orderId: String,
        userId: String,
        userType: UserType,
    ): OrderResponse = OrderQueries.getOrderById(orderId, userId, userType)

    override suspend fun updateOrderStatus(
        userId: String,
        orderId: String,
        status: OrderStatus,
    ): OrderResponse {
        val result =
            query {
                userId.requireNotBlank("User ID")
                orderId.requireNotBlank("Order ID")

                val order =
                    OrderDAO.find { OrderTable.id eq orderId.entityID(OrderTable) }
                        .forUpdate()
                        .firstOrNull()
                        ?: throw ValidationException(Message.Orders.NOT_FOUND)
                val user = UserDAO.findById(userId) ?: throw ValidationException(Message.Errors.NOT_FOUND)

                requireOrderAccess(order.userId.value, order.shopId?.value, userId, user.userType)

                val offlineShipment =
                    status == OrderStatus.SHIPPED &&
                        order.status == OrderStatus.CONFIRMED &&
                        order.paymentMethod in listOf(PaymentMethod.COD, PaymentMethod.BANK_TRANSFER)
                if (!OrderStatus.canTransitionTo(order.status, status) && !offlineShipment) {
                    throw ValidationException(Message.Orders.INVALID_STATUS)
                }
                if (status == OrderStatus.PAID) {
                    // PAID is owned by the payment pipeline so payout, loyalty,
                    // digital delivery and reservation finalization cannot be skipped.
                    throw ValidationException(
                        "Order cannot be marked paid directly; confirm the payment instead",
                    )
                }

                val fromStatus = order.status
                if (status == OrderStatus.CANCELED) {
                    OrderCancellation.cancel(order, notes = "Status updated by user", changedBy = userId)
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
                    OrderEvents.statusChangedTx(order, fromStatus, status, order.trackingNumber)
                }
                order.toOrderResponse(
                    OrderItemDAO.itemsForOrder(order.id).toItemResponses(),
                    includeSensitive = user.userType == UserType.CUSTOMER,
                    forCustomer = user.userType == UserType.CUSTOMER,
                )
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

                val order =
                    OrderDAO.find { OrderTable.id eq orderId.entityID(OrderTable) }
                        .forUpdate()
                        .firstOrNull()
                        ?: throw ValidationException(Message.Orders.NOT_FOUND)

                requireOrderAccess(order.userId.value, order.shopId?.value, userId, userType)
                if (!OrderStatus.canBeCanceled(order.status)) throw ValidationException(Message.Orders.CANNOT_CANCEL)

                val fromStatus = order.status
                OrderCancellation.cancel(order, notes = reason, changedBy = userId)
                OrderEvents.statusChangedTx(order, fromStatus, OrderStatus.CANCELED)
                order.toOrderResponse(
                    OrderItemDAO.itemsForOrder(order.id).toItemResponses(),
                    includeSensitive = userType == UserType.CUSTOMER,
                    forCustomer = userType == UserType.CUSTOMER,
                )
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
                val order =
                    OrderDAO.find { OrderTable.id eq orderId.entityID(OrderTable) }
                        .forUpdate()
                        .firstOrNull()
                        ?: throw ValidationException(Message.Orders.NOT_FOUND)
                val user = UserDAO.findById(userId) ?: throw ValidationException(Message.Errors.NOT_FOUND)
                requireOrderAccess(order.userId.value, order.shopId?.value, userId, user.userType)
                val offlineShipment =
                    order.status == OrderStatus.CONFIRMED &&
                        order.paymentMethod in listOf(PaymentMethod.COD, PaymentMethod.BANK_TRANSFER)
                if (!OrderStatus.canTransitionTo(order.status, OrderStatus.SHIPPED) && !offlineShipment) {
                    throw ValidationException(Message.Orders.INVALID_STATUS)
                }
                val fromStatus = order.status
                order.status = OrderStatus.SHIPPED
                order.shippingDate = currentUtc()
                order.trackingNumber = trackingNumber
                logStatusChange(order.id.value, OrderStatus.SHIPPED, "Shipped via $trackingNumber", userId)
                OrderEvents.statusChangedTx(order, fromStatus, OrderStatus.SHIPPED, trackingNumber)
                order.toOrderResponse(
                    OrderItemDAO.itemsForOrder(order.id).toItemResponses(),
                    includeSensitive = false,
                )
            }
        CatalogCache.invalidate()
        return result
    }

    override suspend fun getSellerOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
        query: String?,
        from: Instant?,
        to: Instant?,
    ): PaginatedResponse<OrderResponse> = OrderQueries.getSellerOrders(userId, limit, offset, status, query, from, to)

    override suspend fun getAdminOrders(
        limit: Int,
        offset: Int,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
        query: String?,
    ): PaginatedResponse<OrderResponse> = OrderQueries.getAdminOrders(limit, offset, status, startDate, endDate, query)
}
