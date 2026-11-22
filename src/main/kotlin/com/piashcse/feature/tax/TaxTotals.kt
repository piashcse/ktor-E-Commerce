package com.piashcse.feature.tax

import com.piashcse.utils.common.Money
import java.math.BigDecimal

/**
 * Accumulates line tax for quotes and cart previews so inclusive/exclusive
 * handling is written once instead of in every checkout surface.
 *
 * Monetary values are normalized to two decimals by [normalized].
 */
data class TaxTotals(
    val tax: BigDecimal = BigDecimal.ZERO,
    val exclusiveTax: BigDecimal = BigDecimal.ZERO,
    val inclusive: Boolean = false,
) {
    fun add(
        amount: BigDecimal,
        quote: TaxQuote,
    ): TaxTotals {
        val applied = TaxService.applyTax(amount, quote)
        return copy(
            tax = tax.add(applied.tax),
            exclusiveTax = if (quote.inclusive) exclusiveTax else exclusiveTax.add(applied.tax),
            inclusive = inclusive || quote.inclusive,
        )
    }

    fun normalized(): TaxTotals =
        copy(
            tax = Money.scale2(tax),
            exclusiveTax = Money.scale2(exclusiveTax),
        )
}
