package com.piashcse.service

import com.piashcse.constants.AppConstants
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductVariantDAO
import com.piashcse.utils.common.Money
import java.math.BigDecimal

data class PriceBreakdown(
    val subTotal: BigDecimal,
    val shippingTotal: BigDecimal,
    val taxAmount: BigDecimal,
    val discount: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal,
)

/** Single place for cart/checkout/order totals — replaces 4x duplicated math. */
object PricingService {
    private val taxRate: BigDecimal get() = Money.of(AppConstants.DEFAULT_TAX_PERCENTAGE)

    /** Variant price wins, then the product's discounted price, then list price. */
    fun unitPriceUsd(
        product: ProductDAO,
        variant: ProductVariantDAO?,
    ): BigDecimal = variant?.effectivePrice ?: product.discountPrice ?: product.price

    fun subtotal(lines: List<Pair<BigDecimal, Int>>): BigDecimal =
        Money.scale2(lines.fold(BigDecimal.ZERO) { acc, (price, qty) -> acc.add(price.multiply(BigDecimal(qty))) })

    fun shipping(
        pricePerShop: BigDecimal,
        shopCount: Int,
    ): BigDecimal = Money.scale2(pricePerShop.multiply(BigDecimal(shopCount.coerceAtLeast(0))))

    fun tax(subTotal: BigDecimal): BigDecimal = Money.tax(subTotal, taxRate)

    /** Converts a USD amount into the order currency and rounds to cents. */
    fun toCurrency(
        amountUsd: BigDecimal,
        fxRate: BigDecimal,
    ): BigDecimal = Money.scale2(amountUsd.multiply(fxRate))

    /** Line total in the order currency: convert the unit price, then multiply by quantity. */
    fun lineTotal(
        unitPriceUsd: BigDecimal,
        quantity: Int,
        fxRate: BigDecimal = BigDecimal.ONE,
    ): BigDecimal = Money.unitTotal(toCurrency(unitPriceUsd, fxRate), quantity)

    fun breakdown(
        subTotal: BigDecimal,
        shippingTotal: BigDecimal,
        discount: BigDecimal = BigDecimal.ZERO,
    ): PriceBreakdown {
        val taxAmount = tax(subTotal)
        val total = Money.scale2(subTotal.add(shippingTotal).add(taxAmount).subtract(discount).max(BigDecimal.ZERO))
        return PriceBreakdown(
            subTotal = Money.scale2(subTotal),
            shippingTotal = Money.scale2(shippingTotal),
            taxAmount = taxAmount,
            discount = Money.scale2(discount),
            total = total,
        )
    }
}
