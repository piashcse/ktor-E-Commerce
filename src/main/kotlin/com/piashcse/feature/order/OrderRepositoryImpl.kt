package com.piashcse.feature.order

import com.piashcse.constants.*
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.OrderPlacedEvent
import com.piashcse.event.OrderStatusChangedEvent
import com.piashcse.event.OutboxPublisher
import com.piashcse.feature.i18n.I18nService
import com.piashcse.feature.loyalty.LoyaltyService
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
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

private val i18n = I18nService()

class OrderRepositoryImpl(
    private val fxService: FxService = FxService(),
    private val taxService: TaxService = TaxService(),
    private val promotionService: PromotionService = PromotionService(),
    private val loyaltyService: LoyaltyService = LoyaltyService(),
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

    private fun generatePickupCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { alphabet[kotlin.random.Random.nextInt(alphabet.length)] }.joinToString("")
    }

    private fun generateOrderNumber(
        datePrefix: String,
        sequenceNumber: Int,
    ) = "ORD-$datePrefix-${sequenceNumber.toString().padStart(4, '0')}-${UUID.randomUUID().toString().take(8).uppercase()}"

    private fun parseOrderStatus(raw: String): OrderStatus =
        runCatching { OrderStatus.valueOf(raw.trim().uppercase()) }.getOrElse {
            throw ValidationException(Message.Errors.invalidParameter("status", raw))
        }

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

        // Per-user limit is enforced against distinct checkouts. Split orders
        // create one row per shop but share a checkout reference.
        if (userId != null) {
            val usedByUser =
                CouponUsageDAO.find {
                    (CouponUsageTable.couponId eq coupon.id) and (CouponUsageTable.userId eq userId.entityID(UserTable))
                }.toList()
                    .map { it.checkoutReference ?: it.id.value }
                    .distinct()
                    .size
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

    private fun couponMatches(
        product: ProductDAO,
        coupon: CouponDAO,
    ): Boolean =
        (coupon.shopId?.value == null || product.shopId?.value == coupon.shopId?.value) &&
            (coupon.categoryId?.value == null || product.categoryId.value == coupon.categoryId?.value)

    private fun unitPriceUsd(
        product: ProductDAO,
        variant: ProductVariantDAO?,
    ): BigDecimal = variant?.effectivePrice ?: product.discountPrice ?: product.price

    /**
     * USD subtotal of cart lines the coupon actually applies to. A shop- or
     * category-scoped coupon must never discount unrelated lines.
     */
    private fun couponEligibleUsd(
        coupon: CouponDAO,
        items: List<CartItemDAO>,
        products: Map<String, ProductDAO>,
        variants: Map<String, ProductVariantDAO>,
    ): BigDecimal =
        items.fold(BigDecimal.ZERO) { acc, item ->
            val product = products[item.productId.value] ?: return@fold acc
            if (!couponMatches(product, coupon)) return@fold acc
            val variant = item.variantId?.value?.let { variants[it] }
            acc.add(Money.unitTotal(unitPriceUsd(product, variant), item.quantity))
        }.let { Money.scale2(it) }

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

    private data class CheckoutShipping(
        val name: String,
        val price: BigDecimal,
    )

    private data class CheckoutContext(
        val cartItems: List<CartItemDAO>,
        val shippingAddress: ShippingAddressDAO?,
        val contactEmail: String,
        val fullAddress: String,
        val shippingMethod: CheckoutShipping,
        val pickupWarehouse: WarehouseDAO?,
        val taxCountry: String,
        val taxRegion: String?,
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
                    val checkoutReference = UUID.randomUUID().toString()
                    val context = loadCheckoutContext(userId, checkoutRequest)
                    val quote = createOrders(context, userId, checkoutRequest, currency, fxRate)
                    if (checkoutRequest.couponCode != null) {
                        applyCoupon(checkoutRequest.couponCode, quote, context, userId, fxRate, checkoutReference)
                    } else {
                        promotionService.evaluateTx(quote.totalUsdSubTotal)?.let { promotion ->
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

        val pickup = checkoutRequest.fulfillmentType == FulfillmentType.PICKUP
        val pickupWarehouse =
            if (pickup) {
                val id =
                    checkoutRequest.pickupWarehouseId?.takeIf { it.isNotBlank() }
                        ?: throw ValidationException("pickupWarehouseId is required for pickup orders")
                val warehouse =
                    WarehouseDAO.findById(id)
                        ?: throw ValidationException("Pickup location not found")
                if (!warehouse.isActive || !warehouse.supportsPickup) {
                    throw ValidationException("Pickup location is not available")
                }
                warehouse
            } else {
                null
            }

        val shippingAddress =
            if (pickup) {
                null
            } else {
                val address =
                    ShippingAddressDAO.findById(
                        checkoutRequest.shippingAddressId
                            ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND),
                    ) ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
                if (address.userId.value != userId) {
                    throw ValidationException(Message.Orders.SHIPPING_ADDRESS_UNAUTHORIZED)
                }
                address
            }
        val shippingMethod =
            if (pickup) {
                CheckoutShipping("PICKUP", BigDecimal.ZERO.setScale(2))
            } else {
                val method =
                    ShippingMethodDAO.findById(
                        checkoutRequest.shippingMethodId
                            ?: throw ValidationException(Message.Orders.SHIPPING_METHOD_NOT_FOUND),
                    ) ?: throw ValidationException(Message.Orders.SHIPPING_METHOD_NOT_FOUND)
                CheckoutShipping(method.name, method.price)
            }
        val deliveryAddress =
            shippingAddress ?: if (pickup) null else throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)

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
        if (pickupWarehouse != null) {
            if (itemsByShop.size != 1) throw ValidationException("Pickup orders must contain items from one shop")
            if (pickupWarehouse.shopId.value != itemsByShop.keys.first()) {
                throw ValidationException("Pickup location does not belong to the selling shop")
            }
        }

        val fullAddress =
            if (pickupWarehouse != null) {
                buildString {
                    append("Pickup at ${pickupWarehouse.name}\n")
                    append("${pickupWarehouse.address ?: ""}, ${pickupWarehouse.city ?: ""}\n")
                    append(pickupWarehouse.country ?: "")
                }
            } else {
                val address = deliveryAddress ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
                buildString {
                    append("${address.firstName} ${address.lastName}\n")
                    append("${address.streetAddress}, ${address.city}, ${address.state ?: ""}\n")
                    append("${address.country}, ${address.zipCode}\n")
                    append("Phone: ${maskPhone(address.phoneNumber)}")
                }
            }
        val contactEmail = deliveryAddress?.email ?: UserDAO.findById(userId)?.email.orEmpty()
        val taxCountry =
            pickupWarehouse?.country ?: deliveryAddress?.country
                ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
        val taxRegion = if (pickupWarehouse != null) null else deliveryAddress?.state

        return CheckoutContext(
            cartItems = cartItems,
            shippingAddress = shippingAddress,
            contactEmail = contactEmail,
            fullAddress = fullAddress,
            shippingMethod = shippingMethod,
            pickupWarehouse = pickupWarehouse,
            taxCountry = taxCountry,
            taxRegion = taxRegion,
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
        var allDigital = true
        var allFreeShipping = true
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
                this.contactEmail = context.contactEmail
                this.paymentMethod = checkoutRequest.paymentMethod
                this.notes = checkoutRequest.notes
                this.currency = currency
                this.fxRate = fxRate
                this.businessTaxId = businessTaxId
                this.taxExempt = taxService.isExempt(userId, businessTaxId, context.taxCountry)
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

        items.forEach { cartItem ->
            val product =
                context.productsMap[cartItem.productId.value]
                    ?: cartItem.productId.value.throwNotFound("Product")
            val variant = resolveCheckoutVariant(product, cartItem, context.variantsMap)
            val pickupWarehouseId = context.pickupWarehouse?.id?.value
            val available =
                if (pickupWarehouseId != null) {
                    product.effectiveStockForAt(pickupWarehouseId, variant?.id?.value)
                } else {
                    product.effectiveStockFor(variant?.id?.value, forUpdate = true)
                }
            if (available < cartItem.quantity) {
                throw ValidationException(Message.Validation.insufficientStock(product.name, available))
            }

            // Product prices are stored in USD; persist the order-currency unit
            // price so refunds and payouts use the same unit as the order.
            if (!product.isDigital) allDigital = false
            if (!product.isDigital && !product.freeShipping) allFreeShipping = false
            val unitPriceUsd = variant?.effectivePrice ?: product.discountPrice ?: product.price
            val unitPrice = Money.scale2(unitPriceUsd.multiply(fxRate))
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

            if (pickupWarehouseId != null) {
                product.decrementStockAt(pickupWarehouseId, variant?.id?.value, cartItem.quantity)
            } else if (variant == null) {
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
        val ship =
            if (allDigital || context.pickupWarehouse != null || allFreeShipping) {
                BigDecimal.ZERO.setScale(2)
            } else {
                Money.scale2(context.shippingMethod.price.multiply(fxRate))
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
        val coupon =
            validateCoupon(
                code,
                quote.totalUsdSubTotal,
                forUpdate = true,
                userId = userId,
                shopIds = context.itemsByShop.keys,
                categoryIds = context.productsMap.values.map { it.categoryId.value }.toSet(),
            )
        val eligibleUsd =
            couponEligibleUsd(coupon, context.cartItems, context.productsMap, context.variantsMap)
        if (eligibleUsd <= BigDecimal.ZERO) {
            throw ValidationException("Coupon is not valid for the products in this order")
        }
        if (eligibleUsd < coupon.minOrderAmount) {
            throw ValidationException(Message.Orders.couponMinOrderAmount(coupon.minOrderAmount.toPlainString()))
        }
        val discountUsd = calculateCouponDiscount(coupon, eligibleUsd)
        consumeCoupon(coupon, userId, quote.orders, checkoutReference)
        val eligibleByOrder =
            quote.orders.associate { order ->
                val orderItems = context.itemsByShop[order.shopId?.value].orEmpty()
                order.id.value to couponEligibleUsd(coupon, orderItems, context.productsMap, context.variantsMap)
            }
        val totalEligible = eligibleByOrder.values.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
        val discount = Money.scale2(discountUsd.multiply(fxRate))
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
            order.total = Money.scale2(order.subTotal.add(order.shippingCost).subtract(orderDiscount).max(BigDecimal.ZERO))
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

        // Cap every redemption before debiting it; never burn points/credit that
        // cannot be applied.
        val pointsToRedeem =
            if (points > 0) {
                points.coerceAtMost(
                    remainingUsd.multiply(BigDecimal(100)).setScale(0, RoundingMode.DOWN).toInt(),
                )
            } else {
                0
            }
        val pointsUsd =
            BigDecimal(pointsToRedeem)
                .divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
        remainingUsd = remainingUsd.subtract(pointsUsd).max(BigDecimal.ZERO)
        val creditToRedeem = creditUsd?.min(remainingUsd)?.max(BigDecimal.ZERO) ?: BigDecimal.ZERO
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
            val discount = order.discountAmount
            var allTax = BigDecimal.ZERO
            var exclusiveTax = BigDecimal.ZERO
            items.forEach { item ->
                val discountShare =
                    if (order.subTotal > BigDecimal.ZERO) {
                        Money.proportionalSplit(discount, item.total, order.subTotal)
                    } else {
                        BigDecimal.ZERO
                    }
                // Persist the line-level share so order/item responses and
                // downstream settlement do not report a zero discount.
                item.discountAmount = discountShare
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

            val pickup = checkoutRequest.fulfillmentType == FulfillmentType.PICKUP
            val pickupWarehouse =
                if (pickup) {
                    val id =
                        checkoutRequest.pickupWarehouseId?.takeIf { it.isNotBlank() }
                            ?: throw ValidationException("pickupWarehouseId is required for pickup orders")
                    val warehouse =
                        WarehouseDAO.findById(id)
                            ?: throw ValidationException("Pickup location not found")
                    if (!warehouse.isActive || !warehouse.supportsPickup) {
                        throw ValidationException("Pickup location is not available")
                    }
                    warehouse
                } else {
                    null
                }
            val shippingMethod =
                if (pickup) {
                    null
                } else {
                    ShippingMethodDAO.findById(
                        checkoutRequest.shippingMethodId
                            ?: throw ValidationException(Message.Orders.SHIPPING_METHOD_NOT_FOUND),
                    ) ?: throw ValidationException(Message.Orders.SHIPPING_METHOD_NOT_FOUND)
                }
            val shippingAddress =
                if (pickup) {
                    null
                } else {
                    ShippingAddressDAO.findById(
                        checkoutRequest.shippingAddressId
                            ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND),
                    ) ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
                }
            if (shippingAddress != null && shippingAddress.userId.value != userId) {
                throw ValidationException(Message.Orders.SHIPPING_ADDRESS_UNAUTHORIZED)
            }
            val taxCountry =
                pickupWarehouse?.country ?: shippingAddress?.country
                    ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
            val taxRegion = if (pickupWarehouse != null) null else shippingAddress?.state

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

            val subUsd = PricingService.subtotal(cartItems.map { unitPriceUsd(it) to it.quantity })
            val sub = Money.scale2(subUsd.multiply(fxRate))
            val physicalProducts =
                cartItems.mapNotNull { productsMap[it.productId.value] }.filter { !it.isDigital }
            val shopsWithShipping =
                physicalProducts
                    .groupBy { it.shopId?.value }
                    .count { (_, products) -> products.any { !it.freeShipping } }
            val ship =
                if (pickup || physicalProducts.isEmpty() || shopsWithShipping == 0) {
                    BigDecimal.ZERO.setScale(2)
                } else {
                    val method =
                        shippingMethod ?: throw ValidationException(Message.Orders.SHIPPING_METHOD_NOT_FOUND)
                    Money.scale2(PricingService.shipping(method.price, shopsWithShipping).multiply(fxRate))
                }
            var tax = BigDecimal.ZERO
            var exclusiveTax = BigDecimal.ZERO
            var taxLines: List<TaxLineResponse> = mutableListOf()
            cartItems.forEach { cartItem ->
                val product =
                    productsMap[cartItem.productId.value]
                        ?: throw ValidationException(Message.Orders.PRODUCT_NOT_FOUND)
                val line = Money.scale2(Money.unitTotal(unitPriceUsd(cartItem), cartItem.quantity).multiply(fxRate))
                val quote =
                    taxService.resolve(
                        country = taxCountry,
                        region = taxRegion,
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
                    taxInclusive = taxLines.any { it.inclusive },
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

            var eligibleCouponProductIds: Set<String>? = null
            checkoutRequest.couponCode?.let {
                val coupon =
                    validateCoupon(
                        it,
                        subUsd,
                        userId = userId,
                        shopIds = productsMap.values.mapNotNull { product -> product.shopId?.value }.toSet(),
                        categoryIds = productsMap.values.map { product -> product.categoryId.value }.toSet(),
                    )
                val eligibleUsd = couponEligibleUsd(coupon, cartItems, productsMap, variantsMap)
                if (eligibleUsd <= BigDecimal.ZERO) {
                    throw ValidationException("Coupon is not valid for the products in this cart")
                }
                if (eligibleUsd < coupon.minOrderAmount) {
                    throw ValidationException(Message.Orders.couponMinOrderAmount(coupon.minOrderAmount.toPlainString()))
                }
                eligibleCouponProductIds =
                    cartItems
                        .mapNotNull { item -> productsMap[item.productId.value] }
                        .filter { product -> couponMatches(product, coupon) }
                        .map { product -> product.id.value }
                        .toSet()
                val discountUsd = calculateCouponDiscount(coupon, eligibleUsd)
                val discount = Money.scale2(discountUsd.multiply(fxRate))
                val total = Money.scale2(sub.add(ship).add(exclusiveTax).subtract(discount).max(BigDecimal.ZERO))
                response =
                    response.copy(
                        discountAmount = discount.toPlainString(),
                        total = total.toPlainString(),
                    )
            }
            val preview =
                loyaltyService.redemptionPreviewTx(
                    userId = userId,
                    points = checkoutRequest.usePoints,
                    walletCreditUsd = checkoutRequest.walletCredit,
                    giftCardCode = checkoutRequest.giftCardCode,
                )
            val responseTotal = BigDecimal(response.total)
            val totalUsd = responseTotal.divide(fxRate, 2, RoundingMode.HALF_UP)
            val walletUsd = preview.pointsUsd.add(preview.creditUsd).min(totalUsd)
            val giftUsd = preview.giftCardUsd.min(totalUsd.subtract(walletUsd).max(BigDecimal.ZERO))
            val walletDiscount = Money.scale2(walletUsd.multiply(fxRate)).min(responseTotal)
            val giftDiscount = Money.scale2(giftUsd.multiply(fxRate)).min(responseTotal.subtract(walletDiscount))
            response =
                response.copy(
                    walletDiscount = walletDiscount.toPlainString(),
                    giftCardDiscount = giftDiscount.toPlainString(),
                    total =
                        Money.scale2(
                            responseTotal.subtract(walletDiscount).subtract(giftDiscount).max(BigDecimal.ZERO),
                        ).toPlainString(),
                    pointsBalance = preview.pointsAvailable,
                    storeCreditBalance = preview.creditAvailable.toPlainString(),
                )
            val locale = com.piashcse.constants.Localization.normalize(checkoutRequest.locale)
            response =
                response.copy(
                    locale = locale,
                    formattedSubTotal = i18n.formatMoney(BigDecimal(response.subTotal), currency, locale),
                    formattedShipping = i18n.formatMoney(BigDecimal(response.shippingCost), currency, locale),
                    formattedTax = i18n.formatMoney(BigDecimal(response.taxAmount), currency, locale),
                    formattedDiscount = i18n.formatMoney(BigDecimal(response.discountAmount), currency, locale),
                    formattedTotal = i18n.formatMoney(BigDecimal(response.total), currency, locale),
                )
            // Coupons/promotions reduce the taxable base; refresh line tax and totals.
            val discountAmount = BigDecimal(response.discountAmount)
            if (discountAmount > BigDecimal.ZERO) {
                val eligibleIds = eligibleCouponProductIds
                val discountBase =
                    if (eligibleIds == null) {
                        sub
                    } else {
                        taxLines
                            .filter { it.productId in eligibleIds }
                            .fold(BigDecimal.ZERO) { acc, line -> acc.add(BigDecimal(line.grossAmount)) }
                    }
                var recalculatedTax = BigDecimal.ZERO
                var recalculatedExclusive = BigDecimal.ZERO
                taxLines =
                    taxLines.map { line ->
                        if (eligibleIds != null && line.productId !in eligibleIds) {
                            recalculatedTax = recalculatedTax.add(BigDecimal(line.taxAmount))
                            if (!line.inclusive) recalculatedExclusive = recalculatedExclusive.add(BigDecimal(line.taxAmount))
                            return@map line
                        }
                        val gross = BigDecimal(line.grossAmount)
                        val share =
                            if (discountBase > BigDecimal.ZERO) {
                                Money.proportionalSplit(discountAmount, gross, discountBase)
                            } else {
                                BigDecimal.ZERO
                            }
                        val amounts =
                            taxService.apply(
                                gross.subtract(share).max(BigDecimal.ZERO),
                                com.piashcse.feature.tax.TaxQuote(
                                    rate = BigDecimal(line.rate),
                                    inclusive = line.inclusive,
                                    taxClassId = line.taxClassId,
                                ),
                            )
                        recalculatedTax = recalculatedTax.add(amounts.tax)
                        if (!line.inclusive) recalculatedExclusive = recalculatedExclusive.add(amounts.tax)
                        line.copy(
                            netAmount = amounts.net.toPlainString(),
                            taxAmount = amounts.tax.toPlainString(),
                            grossAmount = amounts.gross.toPlainString(),
                        )
                    }
                response =
                    response.copy(
                        taxAmount = Money.scale2(recalculatedTax).toPlainString(),
                        total =
                            Money.scale2(
                                sub.subtract(discountAmount).max(BigDecimal.ZERO)
                                    .add(BigDecimal(response.shippingCost))
                                    .add(recalculatedExclusive),
                            ).toPlainString(),
                        // Re-format after the post-discount tax/total recalculation so
                        // formatted fields never disagree with numeric fields.
                        formattedTax = i18n.formatMoney(recalculatedTax, currency, locale),
                        formattedTotal =
                            i18n.formatMoney(
                                Money.scale2(
                                    sub.subtract(discountAmount).max(BigDecimal.ZERO)
                                        .add(BigDecimal(response.shippingCost))
                                        .add(recalculatedExclusive),
                                ),
                                currency,
                                locale,
                            ),
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
                        (OrderTable.contactEmail.lowerCase() eq email.trim().lowercase())
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
            status?.let { query.andWhere { OrderTable.status eq parseOrderStatus(it) } }
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
            status?.let { query.andWhere { OrderTable.status eq parseOrderStatus(it) } }
            startDate?.let { query.andWhere { OrderTable.createdAt greaterEq LocalDateTime.ofInstant(it, ZoneOffset.UTC) } }
            endDate?.let { query.andWhere { OrderTable.createdAt lessEq LocalDateTime.ofInstant(it, ZoneOffset.UTC) } }
            query.orderBy(OrderTable.createdAt to SortOrder.DESC).toOrdersPaginated(limit, offset)
        }
}
