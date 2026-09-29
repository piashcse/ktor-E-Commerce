package com.piashcse.service

import com.piashcse.database.entities.FxRateDAO
import com.piashcse.database.entities.FxRateTable
import com.piashcse.database.entities.SupportedCurrencies
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.eq
import java.math.BigDecimal
import java.math.RoundingMode

/** Converts base-USD amounts to checkout currency using fx_rate (falls back to 1.0). */
object FxService {
    suspend fun rateTo(targetCurrency: String): BigDecimal = query {
        SupportedCurrencies.requireValid(targetCurrency)
        if (targetCurrency.uppercase() == "USD") return@query BigDecimal.ONE
        FxRateDAO.find { FxRateTable.targetCurrency eq targetCurrency.uppercase() }
            .firstOrNull()?.rate ?: BigDecimal.ONE
    }

    suspend fun convertUsd(amountUsd: BigDecimal, targetCurrency: String): BigDecimal {
        val rate = rateTo(targetCurrency)
        return amountUsd.multiply(rate).setScale(2, RoundingMode.HALF_UP)
    }
}
