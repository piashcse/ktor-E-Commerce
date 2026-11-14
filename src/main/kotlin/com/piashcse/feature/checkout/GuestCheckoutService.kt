package com.piashcse.feature.checkout

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.CartItemDAO
import com.piashcse.database.entities.CartItemTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.ProductVariantDAO
import com.piashcse.database.entities.ProductVariantTable
import com.piashcse.database.entities.ShippingAddressDAO
import com.piashcse.database.entities.ShippingMethodDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserProfileDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.feature.config.StorefrontSettings
import com.piashcse.feature.coupon.CouponRepository
import com.piashcse.feature.order.OrderRepository
import com.piashcse.feature.tax.TaxService
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.GuestCheckoutRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.service.FxService
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Guest checkout reuses the normal order pipeline:
 * 1. find/create an unverified guest CUSTOMER for the email,
 * 2. create a temporary cart + shipping address,
 * 3. call the same `placeOrder` service used by signed-in customers.
 *
 * If the email belongs to a verified account the request is rejected so guests
 * can never place orders into someone else's account. A guest can later claim
 * the account by registering and verifying the OTP (isGuest is cleared).
 */
class GuestCheckoutService(
    private val orderRepository: OrderRepository,
    private val couponRepository: CouponRepository,
) {
    private val taxService = TaxService()
    private val fxService = FxService()

    /**
     * Server-side quote for the guest checkout page so the total shown before
     * submitting matches the order that will be placed (tax + shipping + promo).
     */
    suspend fun quote(request: GuestCheckoutRequest): CheckoutSummaryResponse {
        if (request.items.isEmpty()) throw ValidationException(Message.Cart.EMPTY_CART)
        val currency = request.currency.uppercase()
        val fxRate = fxService.rateTo(currency)
        val couponCode = request.couponCode?.trim()?.takeIf { it.isNotBlank() }
        val coupon = couponCode?.let { couponRepository.getCouponByCode(it) }

        return query {
            val products =
                ProductDAO.find {
                    ProductTable.id inList request.items.map { it.productId }.distinct().map { it.entityID(ProductTable) }
                }.associateBy { it.id.value }
            val variantIds = request.items.mapNotNull { it.variantId?.takeIf(String::isNotBlank) }.distinct()
            val variants =
                if (variantIds.isEmpty()) {
                    emptyMap()
                } else {
                    ProductVariantDAO.find {
                        ProductVariantTable.id inList variantIds.map { it.entityID(ProductVariantTable) }
                    }.associateBy { it.id.value }
                }
            val shippingMethod =
                ShippingMethodDAO.findById(request.shippingMethodId)
                    ?: throw ValidationException(Message.Orders.SHIPPING_METHOD_NOT_FOUND)

            var subtotalUsd = BigDecimal.ZERO
            var subTotal = BigDecimal.ZERO
            var tax = BigDecimal.ZERO
            var exclusiveTax = BigDecimal.ZERO
            var itemCount = 0
            val shops = mutableSetOf<String>()
            val shopsWithShipping = mutableSetOf<String>()

            request.items.forEach { item ->
                val product =
                    products[item.productId]
                        ?: throw ValidationException(Message.Orders.PRODUCT_NOT_FOUND)
                if (product.deletedAt != null || product.status != ProductStatus.ACTIVE) {
                    throw ValidationException(Message.Products.OUT_OF_STOCK)
                }
                val variant =
                    item.variantId?.takeIf(String::isNotBlank)?.let { id ->
                        variants[id] ?: id.throwNotFound("Product variant")
                    }
                if (variant != null && variant.productId.value != product.id.value) {
                    throw ValidationException("Variant does not belong to this product")
                }
                val unitUsd = variant?.effectivePrice ?: product.discountPrice ?: product.price
                val lineUsd = Money.unitTotal(unitUsd, item.quantity)
                subtotalUsd = subtotalUsd.add(lineUsd)
                itemCount += item.quantity
                product.shopId?.value?.let { shopId ->
                    shops.add(shopId)
                    if (!product.isDigital && !product.freeShipping) shopsWithShipping.add(shopId)
                }

                val line = Money.scale2(lineUsd.multiply(fxRate))
                subTotal = subTotal.add(line)
                val taxQuote =
                    taxService.resolve(
                        country = request.country,
                        region = request.state,
                        categoryId = product.categoryId.value,
                        taxClassId = product.taxClassId?.value,
                    )
                val amounts = taxService.apply(line, taxQuote)
                tax = tax.add(amounts.tax)
                if (!taxQuote.inclusive) exclusiveTax = exclusiveTax.add(amounts.tax)
            }

            val shippingUsd =
                if (shopsWithShipping.isEmpty() || StorefrontSettings.freeShippingReached(subtotalUsd)) {
                    BigDecimal.ZERO.setScale(2)
                } else {
                    Money.scale2(shippingMethod.price.multiply(BigDecimal(shopsWithShipping.size)))
                }
            val shipping = Money.scale2(shippingUsd.multiply(fxRate))

            var discount = BigDecimal.ZERO.setScale(2)
            var couponError: String? = null
            if (couponCode != null) {
                val now = LocalDateTime.now()
                couponError =
                    when {
                        coupon == null || !coupon.isActive -> "Coupon not found or inactive"
                        now.isBefore(LocalDateTime.parse(coupon.startDate)) -> "Coupon is not active yet"
                        now.isAfter(LocalDateTime.parse(coupon.endDate)) -> "Coupon has expired"
                        coupon.usageLimit != null && coupon.usageCount >= coupon.usageLimit -> "Coupon usage limit reached"
                        BigDecimal(coupon.minOrderAmount) > subtotalUsd ->
                            "Coupon requires a minimum order of ${coupon.minOrderAmount}"
                        coupon.shopId != null && coupon.shopId !in shops -> "Coupon is not valid for these shops"
                        coupon.categoryId != null &&
                            products.values.none { it.categoryId.value == coupon.categoryId } ->
                            "Coupon is not valid for these products"
                        else -> null
                    }
                if (couponError == null && coupon != null) {
                    val eligibleUsd = subtotalUsd
                    val raw =
                        when (coupon.discountType) {
                            com.piashcse.constants.CouponDiscountType.PERCENTAGE ->
                                Money.percentOf(eligibleUsd, BigDecimal(coupon.discountValue))
                            com.piashcse.constants.CouponDiscountType.FIXED -> Money.scale2(BigDecimal(coupon.discountValue))
                        }
                    val capped = coupon.maxDiscountAmount?.let { raw.min(BigDecimal(it)) } ?: raw
                    discount = Money.scale2(capped.min(eligibleUsd).multiply(fxRate))
                }
            }

            val total =
                Money.scale2(
                    subTotal.add(shipping).add(exclusiveTax).subtract(discount).max(BigDecimal.ZERO),
                )
            CheckoutSummaryResponse(
                subTotal = subTotal.toPlainString(),
                shippingCost = shipping.toPlainString(),
                taxAmount = tax.toPlainString(),
                discountAmount = discount.toPlainString(),
                total = total.toPlainString(),
                itemCount = itemCount,
                currency = currency,
                taxInclusive = false,
                couponCode = couponCode,
                couponValid = couponCode?.let { couponError == null },
                couponError = couponError,
                freeShippingThreshold =
                    Money.scale2(StorefrontSettings.freeShippingThreshold.multiply(fxRate)).toPlainString(),
                freeShippingRemaining =
                    Money.scale2(StorefrontSettings.freeShippingRemaining(subtotalUsd).multiply(fxRate)).toPlainString(),
                locale = request.locale,
            )
        }
    }

    suspend fun placeOrder(request: GuestCheckoutRequest): List<OrderResponse> {
        val normalizedEmail = request.email.trim().lowercase()

        val (guestUserId, addressId) =
            query {
                val existingUsers = UserDAO.find { UserTable.email eq normalizedEmail }.toList()
                if (existingUsers.any { it.isVerified }) {
                    throw ValidationException("This email is registered. Please sign in to place the order.")
                }
                val guest =
                    existingUsers.find { it.userType == UserType.CUSTOMER }?.also {
                        if (!it.isGuest) {
                            throw ValidationException("This email is registered. Please sign in to place the order.")
                        }
                    } ?: UserDAO.new {
                        this.email = normalizedEmail
                        userType = UserType.CUSTOMER
                        password =
                            BCrypt.withDefaults().hashToString(
                                AppConstants.BCRYPT_COST,
                                UUID.randomUUID().toString().toCharArray(),
                            )
                        isGuest = true
                        isVerified = false
                    }.also { user ->
                        UserProfileDAO.new {
                            userId = user.id
                            firstName = request.firstName
                            lastName = request.lastName
                            mobile = request.phoneNumber
                        }
                    }

                CartItemDAO.find { CartItemTable.userId eq guest.id }.forEach { it.delete() }
                request.items.forEach { item ->
                    val product =
                        ProductDAO.findById(item.productId) ?: item.productId.throwNotFound("Product")
                    if (product.status != ProductStatus.ACTIVE) {
                        throw ValidationException(Message.Products.OUT_OF_STOCK)
                    }
                    val variant =
                        item.variantId?.takeIf { it.isNotBlank() }?.let { variantId ->
                            val row =
                                ProductVariantDAO.findById(variantId)
                                    ?: variantId.throwNotFound("Product variant")
                            if (row.productId.value != product.id.value) {
                                throw ValidationException("Variant does not belong to this product")
                            }
                            if (row.status != ProductStatus.ACTIVE) {
                                throw ValidationException(Message.Products.OUT_OF_STOCK)
                            }
                            row
                        }
                    CartItemDAO.new {
                        userId = guest.id
                        productId = product.id
                        this.variantId = variant?.id
                        quantity = item.quantity
                    }
                }

                val address =
                    ShippingAddressDAO.new {
                        userId = guest.id
                        firstName = request.firstName
                        lastName = request.lastName
                        this.email = normalizedEmail
                        phoneNumber = request.phoneNumber
                        streetAddress = request.streetAddress
                        city = request.city
                        state = request.state
                        country = request.country
                        zipCode = request.postCode
                        isDefault = true
                    }
                guest.id.value to address.id.value
            }

        return try {
            orderRepository.placeOrder(
                guestUserId,
                CheckoutRequest(
                    shippingAddressId = addressId,
                    shippingMethodId = request.shippingMethodId,
                    paymentMethod = request.paymentMethod,
                    notes = request.notes,
                    idempotencyKey = request.idempotencyKey,
                    couponCode = request.couponCode,
                    currency = request.currency,
                    locale = request.locale,
                    attribution = request.attribution,
                ),
            )
        } finally {
            // Clear on success, replay and failure so no half-built guest cart remains.
            query {
                CartItemDAO.find { CartItemTable.userId eq guestUserId.entityID(UserTable) }
                    .forEach { it.delete() }
            }
        }
    }
}
