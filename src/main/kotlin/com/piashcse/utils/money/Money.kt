package com.piashcse.utils.money

import com.piashcse.constants.AppConstants
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Single source of truth for money math used across cart, order, coupon,
 * commission and discount calculations. All monetary results are rounded to
 * 2 decimal places with HALF_UP rounding.
 */
object Money {
    private const val SCALE = 2
    private val TAX_RATE: BigDecimal = BigDecimal(AppConstants.DEFAULT_TAX_PERCENTAGE.toString())

    fun round(value: BigDecimal): BigDecimal = value.setScale(SCALE, RoundingMode.HALF_UP)

    fun plain(value: BigDecimal): String = round(value).toPlainString()

    fun lineTotal(unitPrice: BigDecimal, quantity: Int): BigDecimal =
        unitPrice.multiply(BigDecimal(quantity)).setScale(SCALE, RoundingMode.HALF_UP)

    fun subtotal(lines: Collection<Pair<BigDecimal, Int>>): BigDecimal =
        lines.fold(BigDecimal.ZERO) { acc, (unitPrice, quantity) -> acc.add(lineTotal(unitPrice, quantity)) }
            .setScale(SCALE, RoundingMode.HALF_UP)

    fun taxOn(amount: BigDecimal): BigDecimal =
        amount.multiply(TAX_RATE).setScale(SCALE, RoundingMode.HALF_UP)

    fun shippingTotal(perShopPrice: BigDecimal, shopCount: Int): BigDecimal =
        perShopPrice.multiply(BigDecimal(shopCount)).setScale(SCALE, RoundingMode.HALF_UP)

    fun total(subtotal: BigDecimal, shipping: BigDecimal, tax: BigDecimal): BigDecimal =
        subtotal.add(shipping).add(tax).setScale(SCALE, RoundingMode.HALF_UP)

    fun percent(amount: BigDecimal, percent: BigDecimal): BigDecimal =
        amount.multiply(percent).divide(BigDecimal(100), SCALE, RoundingMode.HALF_UP)

    fun discountPercent(price: BigDecimal, discountPrice: BigDecimal): BigDecimal? =
        if (discountPrice < price) {
            price.subtract(discountPrice).multiply(BigDecimal(100)).divide(price, SCALE, RoundingMode.HALF_UP)
        } else {
            null
        }

    fun average(total: BigDecimal, count: Int): BigDecimal =
        if (count > 0) total.divide(BigDecimal(count), SCALE, RoundingMode.HALF_UP) else BigDecimal.ZERO
}
