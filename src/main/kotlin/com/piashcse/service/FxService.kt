package com.piashcse.service

import com.piashcse.config.DotEnv
import com.piashcse.database.entities.FxRateDAO
import com.piashcse.database.entities.FxRateTable
import com.piashcse.database.entities.SupportedCurrencies
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.fx.FxRateResponse
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.query
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.math.BigDecimal

/** Converts base-USD amounts to checkout currency using fx_rate (fail-closed when missing/stale). */
object FxService {
    private val maxAgeHours: Long get() = DotEnv.getInt("FX_MAX_AGE_HOURS", 168).toLong()

    suspend fun rateTo(targetCurrency: String): BigDecimal =
        query {
            SupportedCurrencies.requireValid(targetCurrency)
            if (targetCurrency.uppercase() == "USD") return@query BigDecimal.ONE
            val rate =
                FxRateDAO.find { FxRateTable.targetCurrency eq targetCurrency.uppercase() }
                    .firstOrNull() ?: throw ValidationException("FX rate unavailable for ${targetCurrency.uppercase()}")
            val ageHours = java.time.Duration.between(rate.effectiveAt, currentUtc()).toHours()
            if (maxAgeHours > 0 && ageHours > maxAgeHours) {
                throw ValidationException(
                    "FX rate for ${targetCurrency.uppercase()} is stale (${ageHours}h old, max ${maxAgeHours}h)",
                )
            }
            rate.rate
        }

    suspend fun convertUsd(
        amountUsd: BigDecimal,
        targetCurrency: String,
    ): BigDecimal {
        val rate = rateTo(targetCurrency)
        return Money.scale2(amountUsd.multiply(rate))
    }

    suspend fun listRates(): List<FxRateResponse> =
        query {
            FxRateDAO.all().map {
                FxRateResponse(
                    targetCurrency = it.targetCurrency,
                    rate = it.rate.toPlainString(),
                    effectiveAt = it.effectiveAt.toString(),
                )
            }
        }

    suspend fun upsertRate(
        targetCurrency: String,
        rate: BigDecimal,
    ): FxRateResponse =
        query {
            SupportedCurrencies.requireValid(targetCurrency)
            if (rate <= BigDecimal.ZERO) throw ValidationException("FX rate must be positive")
            val target = targetCurrency.uppercase()
            val existing =
                FxRateDAO.find {
                    (FxRateTable.baseCurrency eq "USD") and (FxRateTable.targetCurrency eq target)
                }.firstOrNull()
            val dao =
                existing?.apply {
                    this.rate = rate
                    effectiveAt = currentUtc()
                } ?: FxRateDAO.new {
                    baseCurrency = "USD"
                    this.targetCurrency = target
                    this.rate = rate
                    effectiveAt = currentUtc()
                }
            FxRateResponse(dao.targetCurrency, dao.rate.toPlainString(), dao.effectiveAt.toString())
        }
}
