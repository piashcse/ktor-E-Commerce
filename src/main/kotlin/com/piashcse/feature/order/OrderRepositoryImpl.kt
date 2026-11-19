package com.piashcse.feature.order

import com.piashcse.constants.*
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.OrderPlacedEvent
import com.piashcse.event.OrderStatusChangedEvent
import com.piashcse.event.OutboxPublisher
import com.piashcse.feature.config.StorefrontSettings
import com.piashcse.feature.coupon.CouponLine
import com.piashcse.feature.coupon.CouponRules
import com.piashcse.feature.coupon.toSnapshot
import com.piashcse.feature.i18n.I18nService
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.promotion.PromotionMatch
import com.piashcse.feature.promotion.PromotionService
import com.piashcse.feature.tax.TaxQuote
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

@Suppress("LargeClass", "TooManyFunctions")
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
        includeSensitive: Boolean = true,
        forCustomer: Boolean = false,
    ): PaginatedResponse<OrderResponse> {
        val (totalCount, rows) = toPaginatedList(limit, offset) { OrderDAO.wrapRow(it) }
        val itemsMap = loadItemsForOrders(rows)
        val data =
            rows.map { order ->
                order.toOrderResponse(
                    itemsMap[order.id.value],
                    includeSensitive = includeSensitive,
                    forCustomer = forCustomer,
                )
            }
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

    /**
     * Loads and validates a coupon for the current pricing path. User/usage
     * checks live in [CouponRules] so every checkout entry point agrees.
     */
    private fun loadCoupon(
        code: String,
        forUpdate: Boolean = false,
        userId: String? = null,
    ): CouponDAO {
        val query = CouponDAO.find { (CouponTable.code eq CouponRules.normalize(code)) and (CouponTable.isActive eq true) }
        val coupon =
            (if (forUpdate) query.forUpdate() else query).firstOrNull()
                ?: throw ValidationException(Message.Orders.INVALID_COUPON)
        val usedByUser =
            userId?.let {
                CouponUsageDAO.find {
                    (CouponUsageTable.couponId eq coupon.id) and
                        (CouponUsageTable.userId eq it.entityID(UserTable))
                }.toList()
                    .map { row -> row.checkoutReference ?: row.id.value }
                    .distinct()
                    .size
            } ?: 0
        CouponRules.rejection(coupon.toSnapshot(), currentUtc(), usedByUser)?.let { throw ValidationException(it) }
        return coupon
    }

    private fun cartCouponLines(
        items: List<CartItemDAO>,
        products: Map<String, ProductDAO>,
        variants: Map<String, ProductVariantDAO>,
    ): List<CouponLine> =
        items.mapNotNull { item ->
            products[item.productId.value]?.let { product ->
                CouponLine(
                    productShopId = product.shopId?.value,
                    productCategoryId = product.categoryId.value,
                    unitPriceUsd = PricingService.unitPriceUsd(product, item.variantId?.value?.let(variants::get)),
                    quantity = item.quantity,
                )
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

    private data class CheckoutShipping(
        val name: String,
        val price: BigDecimal,
        val shopId: String? = null,
    )

    private data class Fulfillment(
        val pickupWarehouse: WarehouseDAO?,
        val shippingAddress: ShippingAddressDAO?,
        val shippingMethod: CheckoutShipping,
        val fullAddress: String,
        val contactEmail: String,
        val taxCountry: String,
        val taxRegion: String?,
    )

    /** Shared pickup/delivery resolution used by both order placement and preview. */
    private fun resolveFulfillment(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): Fulfillment {
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
                CheckoutShipping(method.name, method.price, method.shopId?.value)
            }
        val fullAddress =
            if (pickupWarehouse != null) {
                buildString {
                    append("Pickup at ${pickupWarehouse.name}\n")
                    append("${pickupWarehouse.address ?: ""}, ${pickupWarehouse.city ?: ""}\n")
                    append(pickupWarehouse.country ?: "")
                }
            } else {
                val address = shippingAddress ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
                buildString {
                    append("${address.firstName} ${address.lastName}\n")
                    append("${address.streetAddress}, ${address.city}, ${address.state ?: ""}\n")
                    append("${address.country}, ${address.zipCode}\n")
                    append("Phone: ${maskPhone(address.phoneNumber)}")
                }
            }
        val taxCountry =
            pickupWarehouse?.country ?: shippingAddress?.country
                ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
        return Fulfillment(
            pickupWarehouse = pickupWarehouse,
            shippingAddress = shippingAddress,
            shippingMethod = shippingMethod,
            fullAddress = fullAddress,
            contactEmail = shippingAddress?.email ?: UserDAO.findById(userId)?.email.orEmpty(),
            taxCountry = taxCountry,
            taxRegion = if (pickup) null else shippingAddress?.state,
        )
    }

    private fun loadCartCatalog(cartItems: List<CartItemDAO>): Pair<Map<String, ProductDAO>, Map<String, ProductVariantDAO>> {
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
        return productsMap to variantsMap
    }

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

    private fun loadCheckoutContext(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutContext {
        val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
        if (cartItems.isEmpty()) throw ValidationException(Message.Cart.EMPTY_CART)

        val fulfillment = resolveFulfillment(userId, checkoutRequest)
        val (productsMap, variantsMap) = loadCartCatalog(cartItems)

        val itemsByShop =
            cartItems.groupBy {
                productsMap[it.productId.value]?.shopId?.value
                    ?: throw ValidationException(Message.Orders.productDoesNotBelongToShop(it.productId.value))
            }
        validateShopsApproved(itemsByShop.keys)
        fulfillment.shippingMethod.shopId?.let { methodShop ->
            if (itemsByShop.size != 1 || methodShop != itemsByShop.keys.first()) {
                throw ValidationException("Shipping method is not available for these shops")
            }
        }
        fulfillment.pickupWarehouse?.let { warehouse ->
            if (itemsByShop.size != 1) throw ValidationException("Pickup orders must contain items from one shop")
            if (warehouse.shopId.value != itemsByShop.keys.first()) {
                throw ValidationException("Pickup location does not belong to the selling shop")
            }
        }

        return CheckoutContext(
            cartItems = cartItems,
            shippingAddress = fulfillment.shippingAddress,
            contactEmail = fulfillment.contactEmail,
            fullAddress = fulfillment.fullAddress,
            shippingMethod = fulfillment.shippingMethod,
            pickupWarehouse = fulfillment.pickupWarehouse,
            taxCountry = fulfillment.taxCountry,
            taxRegion = fulfillment.taxRegion,
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
            val unitPriceUsd = PricingService.unitPriceUsd(product, variant)
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

            val allocations =
                when {
                    pickupWarehouseId != null ->
                        product.decrementStockAt(pickupWarehouseId, variant?.id?.value, cartItem.quantity)
                    variant == null -> product.decrementStock(cartItem.quantity)
                    else -> product.decrementStockFor(variant.id.value, cartItem.quantity)
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
                    this.expiresAt = currentUtc().plusHours(24)
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
        val coupon = loadCoupon(code, forUpdate = true, userId = userId)
        val snapshot = coupon.toSnapshot()
        val eligibleUsd =
            CouponRules.eligibleSubtotalUsd(
                snapshot,
                cartCouponLines(context.cartItems, context.productsMap, context.variantsMap),
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
                        cartCouponLines(orderItems, context.productsMap, context.variantsMap),
                    )
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

    private data class TaxPreview(
        val tax: BigDecimal,
        val exclusiveTax: BigDecimal,
        val lines: List<TaxLineResponse>,
    )

    private data class CouponPreviewResult(
        val response: CheckoutSummaryResponse,
        val eligibleProductIds: Set<String>?,
    )

    private fun checkoutSubtotalUsd(context: CheckoutContext): BigDecimal =
        PricingService.subtotal(
            context.cartItems.map { cartItem ->
                val product =
                    context.productsMap[cartItem.productId.value]
                        ?: throw ValidationException(Message.Orders.PRODUCT_NOT_FOUND)
                val variant = cartItem.variantId?.value?.let(context.variantsMap::get)
                PricingService.unitPriceUsd(product, variant) to cartItem.quantity
            },
        )

    private fun checkoutShipping(
        context: CheckoutContext,
        subUsd: BigDecimal,
        fxRate: BigDecimal,
    ): BigDecimal {
        val shopsWithShipping =
            context.cartItems
                .mapNotNull { context.productsMap[it.productId.value] }
                .filter { !it.isDigital }
                .groupBy { it.shopId?.value }
                .count { (_, products) -> products.any { !it.freeShipping } }
        return if (context.pickupWarehouse != null ||
            shopsWithShipping == 0 ||
            StorefrontSettings.freeShippingReached(subUsd)
        ) {
            BigDecimal.ZERO.setScale(2)
        } else {
            Money.scale2(PricingService.shipping(context.shippingMethod.price, shopsWithShipping).multiply(fxRate))
        }
    }

    private fun checkoutTaxPreview(
        context: CheckoutContext,
        userId: String,
        businessTaxId: String?,
        fxRate: BigDecimal,
    ): TaxPreview {
        var tax = BigDecimal.ZERO
        var exclusiveTax = BigDecimal.ZERO
        val lines = mutableListOf<TaxLineResponse>()
        context.cartItems.forEach { cartItem ->
            val product =
                context.productsMap[cartItem.productId.value]
                    ?: throw ValidationException(Message.Orders.PRODUCT_NOT_FOUND)
            val variant = cartItem.variantId?.value?.let(context.variantsMap::get)
            val line =
                Money.scale2(
                    Money.unitTotal(PricingService.unitPriceUsd(product, variant), cartItem.quantity).multiply(fxRate),
                )
            val quote =
                taxService.resolve(
                    country = context.taxCountry,
                    region = context.taxRegion,
                    categoryId = product.categoryId.value,
                    taxClassId = product.taxClassId?.value,
                    userId = userId,
                    businessTaxId = businessTaxId,
                )
            val amounts = taxService.apply(line, quote)
            tax = tax.add(amounts.tax)
            if (!quote.inclusive) exclusiveTax = exclusiveTax.add(amounts.tax)
            lines +=
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
        return TaxPreview(Money.scale2(tax), Money.scale2(exclusiveTax), lines)
    }

    private fun couponPreview(
        response: CheckoutSummaryResponse,
        code: String,
        userId: String,
        context: CheckoutContext,
        sub: BigDecimal,
        ship: BigDecimal,
        exclusiveTax: BigDecimal,
        fxRate: BigDecimal,
    ): CouponPreviewResult {
        // A mistyped promo code must never blank the whole totals panel:
        // report validity inline and keep the undiscounted totals.
        return try {
            val snapshot = loadCoupon(code, userId = userId).toSnapshot()
            val lines = cartCouponLines(context.cartItems, context.productsMap, context.variantsMap)
            val eligibleUsd = CouponRules.eligibleSubtotalUsd(snapshot, lines)
            CouponRules.eligibilityError(snapshot, eligibleUsd)?.let { throw ValidationException(it) }
            val discount = Money.scale2(CouponRules.discountUsd(snapshot, eligibleUsd).multiply(fxRate))
            val total = Money.scale2(sub.add(ship).add(exclusiveTax).subtract(discount).max(BigDecimal.ZERO))
            val eligibleProductIds =
                context.productsMap.values
                    .filter { product ->
                        CouponRules.matches(snapshot, product.shopId?.value, product.categoryId.value)
                    }.map { product -> product.id.value }
                    .toSet()
            CouponPreviewResult(
                response.copy(
                    couponCode = code,
                    couponValid = true,
                    discountAmount = discount.toPlainString(),
                    total = total.toPlainString(),
                ),
                eligibleProductIds,
            )
        } catch (error: ValidationException) {
            CouponPreviewResult(
                response.copy(
                    couponCode = code,
                    couponValid = false,
                    couponError = error.message ?: "Coupon is not valid",
                ),
                null,
            )
        }
    }

    /** Coupons/promotions reduce the taxable base; refresh line tax before redemptions. */
    private fun applyDiscountToTax(
        response: CheckoutSummaryResponse,
        taxLines: List<TaxLineResponse>,
        eligibleProductIds: Set<String>?,
        sub: BigDecimal,
    ): CheckoutSummaryResponse {
        val discount = BigDecimal(response.discountAmount)
        if (discount <= BigDecimal.ZERO) return response
        val discountBase =
            if (eligibleProductIds == null) {
                sub
            } else {
                taxLines
                    .filter { it.productId in eligibleProductIds }
                    .fold(BigDecimal.ZERO) { acc, line -> acc.add(BigDecimal(line.grossAmount)) }
            }
        var recalculatedTax = BigDecimal.ZERO
        var recalculatedExclusive = BigDecimal.ZERO
        val recalculatedLines =
            taxLines.map { line ->
                if (eligibleProductIds != null && line.productId !in eligibleProductIds) {
                    recalculatedTax = recalculatedTax.add(BigDecimal(line.taxAmount))
                    if (!line.inclusive) recalculatedExclusive = recalculatedExclusive.add(BigDecimal(line.taxAmount))
                    return@map line
                }
                val gross = BigDecimal(line.grossAmount)
                val share =
                    if (discountBase > BigDecimal.ZERO) {
                        Money.proportionalSplit(discount, gross, discountBase)
                    } else {
                        BigDecimal.ZERO
                    }
                val amounts =
                    taxService.apply(
                        gross.subtract(share).max(BigDecimal.ZERO),
                        TaxQuote(
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
        return response.copy(
            taxAmount = Money.scale2(recalculatedTax).toPlainString(),
            taxLines = recalculatedLines,
            total =
                Money.scale2(
                    sub.subtract(discount).max(BigDecimal.ZERO)
                        .add(BigDecimal(response.shippingCost))
                        .add(recalculatedExclusive),
                ).toPlainString(),
        )
    }

    private fun applyWalletPreview(
        response: CheckoutSummaryResponse,
        request: CheckoutRequest,
        userId: String,
        fxRate: BigDecimal,
    ): CheckoutSummaryResponse {
        val preview =
            loyaltyService.redemptionPreviewTx(
                userId = userId,
                points = request.usePoints,
                walletCreditUsd = request.walletCredit,
                giftCardCode = request.giftCardCode,
            )
        val responseTotal = BigDecimal(response.total)
        val totalUsd = responseTotal.divide(fxRate, 2, RoundingMode.HALF_UP)
        val walletUsd = preview.pointsUsd.add(preview.creditUsd).min(totalUsd)
        val giftUsd = preview.giftCardUsd.min(totalUsd.subtract(walletUsd).max(BigDecimal.ZERO))
        val walletDiscount = Money.scale2(walletUsd.multiply(fxRate)).min(responseTotal)
        val giftDiscount = Money.scale2(giftUsd.multiply(fxRate)).min(responseTotal.subtract(walletDiscount))
        return response.copy(
            walletDiscount = walletDiscount.toPlainString(),
            giftCardDiscount = giftDiscount.toPlainString(),
            total =
                Money.scale2(
                    responseTotal.subtract(walletDiscount).subtract(giftDiscount).max(BigDecimal.ZERO),
                ).toPlainString(),
            pointsBalance = preview.pointsAvailable,
            storeCreditBalance = preview.creditAvailable.toPlainString(),
        )
    }

    private fun formatSummary(
        response: CheckoutSummaryResponse,
        currency: String,
        checkoutRequest: CheckoutRequest,
        fxRate: BigDecimal,
        subUsd: BigDecimal,
    ): CheckoutSummaryResponse {
        val locale = com.piashcse.constants.Localization.normalize(checkoutRequest.locale)
        return response.copy(
            locale = locale,
            freeShippingThreshold =
                Money.scale2(StorefrontSettings.freeShippingThreshold.multiply(fxRate)).toPlainString(),
            freeShippingRemaining =
                Money.scale2(StorefrontSettings.freeShippingRemaining(subUsd).multiply(fxRate)).toPlainString(),
            formattedSubTotal = i18n.formatMoney(BigDecimal(response.subTotal), currency, locale),
            formattedShipping = i18n.formatMoney(BigDecimal(response.shippingCost), currency, locale),
            formattedTax = i18n.formatMoney(BigDecimal(response.taxAmount), currency, locale),
            formattedDiscount = i18n.formatMoney(BigDecimal(response.discountAmount), currency, locale),
            formattedTotal = i18n.formatMoney(BigDecimal(response.total), currency, locale),
        )
    }

    override suspend fun getCheckoutSummary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutSummaryResponse {
        val currency = checkoutRequest.currency.uppercase()
        val fxRate = if (currency != "USD") fxService.rateTo(currency) else BigDecimal.ONE
        return query {
            val context = loadCheckoutContext(userId, checkoutRequest)
            val subUsd = checkoutSubtotalUsd(context)
            val sub = Money.scale2(subUsd.multiply(fxRate))
            val ship = checkoutShipping(context, subUsd, fxRate)
            val taxPreview = checkoutTaxPreview(context, userId, checkoutRequest.businessTaxId, fxRate)

            var response =
                CheckoutSummaryResponse(
                    subTotal = sub.toPlainString(),
                    shippingCost = ship.toPlainString(),
                    taxAmount = taxPreview.tax.toPlainString(),
                    taxInclusive = taxPreview.lines.any { it.inclusive },
                    total = Money.scale2(sub.add(ship).add(taxPreview.exclusiveTax)).toPlainString(),
                    itemCount = context.cartItems.sumOf { it.quantity },
                    currency = currency,
                    taxLines = taxPreview.lines,
                )

            var eligibleCouponProductIds: Set<String>? = null
            val couponCode = checkoutRequest.couponCode
            if (couponCode == null) {
                promotionService.evaluateTx(subUsd, context.itemsByShop.keys)?.let { promotion ->
                    val discount = Money.scale2(promotion.discount.multiply(fxRate))
                    val discountedShipping = if (promotion.freeShipping) BigDecimal.ZERO.setScale(2) else ship
                    response =
                        response.copy(
                            shippingCost = discountedShipping.toPlainString(),
                            discountAmount = discount.toPlainString(),
                            total =
                                Money.scale2(
                                    sub.add(discountedShipping)
                                        .add(taxPreview.exclusiveTax)
                                        .subtract(discount)
                                        .max(BigDecimal.ZERO),
                                ).toPlainString(),
                        )
                }
            } else {
                val preview =
                    couponPreview(
                        response,
                        couponCode,
                        userId,
                        context,
                        sub,
                        ship,
                        taxPreview.exclusiveTax,
                        fxRate,
                    )
                response = preview.response
                eligibleCouponProductIds = preview.eligibleProductIds
            }

            response = applyDiscountToTax(response, taxPreview.lines, eligibleCouponProductIds, sub)
            response = applyWalletPreview(response, checkoutRequest, userId, fxRate)
            formatSummary(response, currency, checkoutRequest, fxRate, subUsd)
        }
    }

    override suspend fun getOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
        query: String?,
        from: Instant?,
        to: Instant?,
    ): PaginatedResponse<OrderResponse> =
        query {
            val orders = OrderTable.selectAll().andWhere { OrderTable.userId eq userId }
            status?.let { orders.andWhere { OrderTable.status eq parseOrderStatus(it) } }
            query?.takeIf { it.isNotBlank() }?.let { term ->
                val pattern = "%${term.trim().lowercase().escapeLike()}%"
                orders.andWhere {
                    (OrderTable.orderNumber.lowerCase() like pattern) or
                        (OrderTable.contactEmail.lowerCase() like pattern)
                }
            }
            from?.let { orders.andWhere { OrderTable.createdAt greaterEq LocalDateTime.ofInstant(it, ZoneOffset.UTC) } }
            to?.let { orders.andWhere { OrderTable.createdAt lessEq LocalDateTime.ofInstant(it, ZoneOffset.UTC) } }
            orders
                .orderBy(OrderTable.createdAt to SortOrder.DESC)
                .toOrdersPaginated(limit, offset, forCustomer = true)
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
                data = orders.map { it.toOrderResponse(itemsMap[it.id.value], forCustomer = true) },
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
            order.toOrderResponse(
                OrderItemDAO.itemsForOrder(order.id).toItemResponses(),
                includeSensitive = false,
                forCustomer = true,
            )
        }

    override suspend fun getOrderById(
        orderId: String,
        userId: String,
        userType: UserType,
    ): OrderResponse =
        query {
            orderId.requireNotBlank("Order ID")
            val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
            requireOrderAccess(order.userId.value, order.shopId?.value, userId, userType)
            order.toOrderResponse(
                OrderItemDAO.itemsForOrder(order.id).toItemResponses(),
                includeSensitive = userType == UserType.CUSTOMER,
                forCustomer = userType == UserType.CUSTOMER,
            )
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

                val order =
                    OrderDAO.find { OrderTable.id eq orderId.entityID(OrderTable) }
                        .forUpdate()
                        .firstOrNull()
                        ?: throw ValidationException(Message.Orders.NOT_FOUND)
                val user = UserDAO.findById(userId) ?: throw ValidationException(Message.Errors.NOT_FOUND)

                requireOrderAccess(order.userId.value, order.shopId?.value, userId, user.userType)

                if (!OrderStatus.canTransitionTo(order.status, status)) {
                    throw ValidationException(Message.Orders.INVALID_STATUS)
                }
                if (status == OrderStatus.PAID) {
                    val hasCompletedPayment =
                        PaymentDAO.find {
                            (PaymentTable.orderId eq order.id) and
                                (PaymentTable.status eq PaymentStatus.COMPLETED)
                        }.any()
                    if (!hasCompletedPayment) {
                        throw ValidationException(
                            "Order cannot be marked paid without a completed payment record",
                        )
                    }
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
                applyOrderCancellation(order, notes = reason, changedBy = userId)
                enqueueStatusChanged(order, fromStatus, OrderStatus.CANCELED)
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
                if (!OrderStatus.canTransitionTo(order.status, OrderStatus.SHIPPED)) {
                    throw ValidationException(Message.Orders.INVALID_STATUS)
                }
                val fromStatus = order.status
                order.status = OrderStatus.SHIPPED
                order.shippingDate = currentUtc()
                order.trackingNumber = trackingNumber
                logStatusChange(order.id.value, OrderStatus.SHIPPED, "Shipped via $trackingNumber", userId)
                enqueueStatusChanged(order, fromStatus, OrderStatus.SHIPPED, trackingNumber)
                order.toOrderResponse(
                    OrderItemDAO.itemsForOrder(order.id).toItemResponses(),
                    includeSensitive = false,
                )
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
        query: String?,
        from: Instant?,
        to: Instant?,
    ): PaginatedResponse<OrderResponse> =
        query {
            val seller =
                try {
                    requireSellerByUserId(userId)
                } catch (_: ForbiddenException) {
                    throw ValidationException(Message.Orders.SELLER_PROFILE_NOT_FOUND)
                }
            val shopId = seller.shopId ?: throw ValidationException(Message.Orders.NO_SHOP_ASSOCIATED)

            val orders = OrderTable.selectAll().andWhere { OrderTable.shopId eq shopId }
            status?.let { orders.andWhere { OrderTable.status eq parseOrderStatus(it) } }
            query?.takeIf { it.isNotBlank() }?.let { term ->
                val pattern = "%${term.trim().lowercase().escapeLike()}%"
                orders.andWhere {
                    (OrderTable.orderNumber.lowerCase() like pattern) or
                        (OrderTable.contactEmail.lowerCase() like pattern)
                }
            }
            from?.let { orders.andWhere { OrderTable.createdAt greaterEq LocalDateTime.ofInstant(it, ZoneOffset.UTC) } }
            to?.let { orders.andWhere { OrderTable.createdAt lessEq LocalDateTime.ofInstant(it, ZoneOffset.UTC) } }
            orders
                .orderBy(OrderTable.createdAt to SortOrder.DESC)
                .toOrdersPaginated(limit, offset, includeSensitive = false)
        }

    override suspend fun getAdminOrders(
        limit: Int,
        offset: Int,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
        query: String?,
    ): PaginatedResponse<OrderResponse> =
        query {
            val orders = OrderTable.selectAll()
            status?.let { orders.andWhere { OrderTable.status eq parseOrderStatus(it) } }
            startDate?.let { orders.andWhere { OrderTable.createdAt greaterEq LocalDateTime.ofInstant(it, ZoneOffset.UTC) } }
            endDate?.let { orders.andWhere { OrderTable.createdAt lessEq LocalDateTime.ofInstant(it, ZoneOffset.UTC) } }
            query?.takeIf { it.isNotBlank() }?.let { term ->
                val pattern = "%${term.trim().lowercase().escapeLike()}%"
                orders.andWhere {
                    (OrderTable.orderNumber.lowerCase() like pattern) or
                        (OrderTable.contactEmail.lowerCase() like pattern)
                }
            }
            orders
                .orderBy(OrderTable.createdAt to SortOrder.DESC)
                .toOrdersPaginated(limit, offset, includeSensitive = false)
        }
}
