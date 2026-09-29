package com.piashcse.utils.common

import java.math.BigDecimal
import java.math.RoundingMode

/** Shared money math — single place for scale/rounding to avoid 4x duplication. */
object Money {
    fun scale2(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)

    fun of(double: Double): BigDecimal = scale2(BigDecimal(double.toString()))

    fun unitTotal(unitPrice: BigDecimal, quantity: Int): BigDecimal = scale2(unitPrice.multiply(BigDecimal(quantity)))

    fun tax(subtotal: BigDecimal, rate: BigDecimal): BigDecimal = scale2(subtotal.multiply(rate))

    fun discountPercent(price: BigDecimal, discountPrice: BigDecimal?): BigDecimal? {
        if (discountPrice == null || discountPrice >= price || price <= BigDecimal.ZERO) return null
        return scale2(price.subtract(discountPrice).multiply(BigDecimal(100)).divide(price, 4, RoundingMode.HALF_UP))
    }

    fun cappedDiscount(discount: BigDecimal, orderAmount: BigDecimal): BigDecimal = discount.min(orderAmount).max(BigDecimal.ZERO)
}
