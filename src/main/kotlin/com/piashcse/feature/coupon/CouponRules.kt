package com.piashcse.feature.coupon

import com.piashcse.constants.CouponDiscountType
import com.piashcse.constants.Message
import com.piashcse.database.entities.CouponDAO
import com.piashcse.utils.common.Money
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Immutable coupon state used by the pricing paths. Keeping the rules pure makes
 * checkout preview, order placement and guest checkout share one implementation.
 */
data class CouponSnapshot(
    val discountType: CouponDiscountType,
    val discountValue: BigDecimal,
    val minOrderAmount: BigDecimal,
    val maxDiscountAmount: BigDecimal?,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val usageLimit: Int?,
    val usageCount: Int,
    val perUserLimit: Int,
    val shopId: String?,
    val categoryId: String?,
    val isActive: Boolean,
)

/** One cart/order line as seen by coupon scoping. */
data class CouponLine(
    val productShopId: String?,
    val productCategoryId: String,
    val unitPriceUsd: BigDecimal,
    val quantity: Int,
)

fun CouponDAO.toSnapshot(): CouponSnapshot =
    CouponSnapshot(
        discountType = discountType,
        discountValue = discountValue,
        minOrderAmount = minOrderAmount,
        maxDiscountAmount = maxDiscountAmount,
        startDate = startDate,
        endDate = endDate,
        usageLimit = usageLimit,
        usageCount = usageCount,
        perUserLimit = perUserLimit,
        shopId = shopId?.value,
        categoryId = categoryId?.value,
        isActive = isActive,
    )

/**
 * Pure coupon rules shared by checkout preview, order placement and guest
 * checkout so validity, scoping and discount math cannot drift.
 */
object CouponRules {
    fun normalize(code: String): String = code.trim().uppercase()

    /** Lifecycle/usage rejection reason, or null when the coupon may be used. */
    fun rejection(
        coupon: CouponSnapshot,
        now: LocalDateTime,
        usedByUser: Int = 0,
    ): String? =
        when {
            !coupon.isActive -> Message.Orders.INVALID_COUPON
            now.isBefore(coupon.startDate) || now.isAfter(coupon.endDate) -> Message.Orders.COUPON_EXPIRED
            coupon.usageLimit?.let { coupon.usageCount >= it } == true -> Message.Orders.COUPON_LIMIT_REACHED
            usedByUser >= coupon.perUserLimit -> Message.Orders.COUPON_LIMIT_REACHED
            else -> null
        }

    /** Cart-dependent rejection reason (scope, minimum spend), or null when applicable. */
    fun eligibilityError(
        coupon: CouponSnapshot,
        eligibleUsd: BigDecimal,
    ): String? =
        when {
            eligibleUsd <= BigDecimal.ZERO -> "Coupon is not valid for the products in this order"
            eligibleUsd < coupon.minOrderAmount ->
                Message.Orders.couponMinOrderAmount(Money.str(coupon.minOrderAmount))
            else -> null
        }

    fun matches(
        coupon: CouponSnapshot,
        productShopId: String?,
        productCategoryId: String,
    ): Boolean =
        (coupon.shopId == null || productShopId == coupon.shopId) &&
            (coupon.categoryId == null || productCategoryId == coupon.categoryId)

    /** USD subtotal of the lines this coupon actually discounts. */
    fun eligibleSubtotalUsd(
        coupon: CouponSnapshot,
        lines: List<CouponLine>,
    ): BigDecimal =
        Money.scale2(
            lines
                .filter { matches(coupon, it.productShopId, it.productCategoryId) }
                .fold(BigDecimal.ZERO) { acc, line -> acc.add(Money.unitTotal(line.unitPriceUsd, line.quantity)) },
        )

    /** Capped, 2dp discount in USD for the eligible amount. */
    fun discountUsd(
        coupon: CouponSnapshot,
        eligibleUsd: BigDecimal,
    ): BigDecimal {
        val raw =
            when (coupon.discountType) {
                CouponDiscountType.PERCENTAGE -> Money.percentOf(eligibleUsd, coupon.discountValue)
                CouponDiscountType.FIXED -> Money.scale2(coupon.discountValue)
            }
        return Money.scale2(Money.cappedDiscount(coupon.maxDiscountAmount?.let { raw.min(it) } ?: raw, eligibleUsd))
    }
}
