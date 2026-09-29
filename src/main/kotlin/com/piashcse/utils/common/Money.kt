package com.piashcse.utils.common

import java.math.BigDecimal
import java.math.RoundingMode

/** Shared money math — single place for scale/rounding to avoid 4x duplication. */
object Money {
    fun scale2(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)

    fun of(double: Double): BigDecimal = scale2(BigDecimal(double.toString()))

    fun unitTotal(
        unitPrice: BigDecimal,
        quantity: Int,
    ): BigDecimal = scale2(unitPrice.multiply(BigDecimal(quantity)))

    fun tax(
        subtotal: BigDecimal,
        rate: BigDecimal,
    ): BigDecimal = scale2(subtotal.multiply(rate))

    fun discountPercent(
        price: BigDecimal,
        discountPrice: BigDecimal?,
    ): BigDecimal? {
        if (discountPrice == null || discountPrice >= price || price <= BigDecimal.ZERO) return null
        return scale2(price.subtract(discountPrice).multiply(BigDecimal(100)).divide(price, 4, RoundingMode.HALF_UP))
    }

    fun cappedDiscount(
        discount: BigDecimal,
        orderAmount: BigDecimal,
    ): BigDecimal = discount.min(orderAmount).max(BigDecimal.ZERO)

    /** `amount * pct / 100` at 2dp (coupon percentage, commission). */
    fun percentOf(
        amount: BigDecimal,
        pct: BigDecimal,
    ): BigDecimal = scale2(amount.multiply(pct.divide(BigDecimal(100), 10, RoundingMode.HALF_UP)))

    /** Seller commission: `subTotal * ratePct / 100` at 2dp. */
    fun commission(
        subTotal: BigDecimal,
        ratePct: BigDecimal,
    ): BigDecimal = percentOf(subTotal, ratePct)

    /** Pro-rata slice of a discount: `discount * part / total` at 2dp. */
    fun proportionalSplit(
        discount: BigDecimal,
        part: BigDecimal,
        total: BigDecimal,
    ): BigDecimal {
        if (total <= BigDecimal.ZERO) return BigDecimal.ZERO.setScale(2)
        return scale2(discount.multiply(part.divide(total, 10, RoundingMode.HALF_UP)))
    }

    /** Average `total / count` at 2dp (0 when empty). */
    fun average(
        total: BigDecimal,
        count: Long,
    ): BigDecimal = if (count <= 0) BigDecimal.ZERO.setScale(2) else total.divide(BigDecimal(count), 2, RoundingMode.HALF_UP)

    /** Render for API responses: `scale2().toPlainString()`. */
    fun str(value: BigDecimal): String = scale2(value).toPlainString()
}
