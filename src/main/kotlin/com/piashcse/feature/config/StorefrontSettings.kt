package com.piashcse.feature.config

import com.piashcse.config.DotEnv
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Storefront rules that both the API response and the UI copy must agree on.
 * Keeping them here prevents the cart/product pages from advertising shipping
 * promises the checkout cannot honour.
 */
object StorefrontSettings {
    val freeShippingThreshold: BigDecimal
        get() =
            DotEnv.get("FREE_SHIPPING_THRESHOLD", "75")
                .toBigDecimalOrNull()
                ?.max(BigDecimal.ZERO)
                ?: BigDecimal("75")

    val returnWindowDays: Int
        get() = DotEnv.getInt("RETURN_WINDOW_DAYS", 30).coerceAtLeast(0)

    fun freeShippingReached(subtotalUsd: BigDecimal): Boolean =
        freeShippingThreshold <= BigDecimal.ZERO || subtotalUsd >= freeShippingThreshold

    fun freeShippingRemaining(subtotalUsd: BigDecimal): BigDecimal =
        freeShippingThreshold
            .subtract(subtotalUsd)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)
}
