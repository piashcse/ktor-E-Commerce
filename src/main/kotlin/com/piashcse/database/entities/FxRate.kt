package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal

object FxRateTable : BaseIdTable("fx_rate") {
    val baseCurrency = varchar("base_currency", 3).default("USD")
    val targetCurrency = varchar("target_currency", 3).index()
    val rate = decimal("rate", 18, 6)
    val effectiveAt = datetime("effective_at")
}

class FxRateDAO(id: EntityID<String>) : BaseEntity(id, FxRateTable) {
    companion object : BaseEntityClass<FxRateDAO>(FxRateTable, FxRateDAO::class.java)

    var baseCurrency by FxRateTable.baseCurrency
    var targetCurrency by FxRateTable.targetCurrency
    var rate by FxRateTable.rate
    var effectiveAt by FxRateTable.effectiveAt
}

/** Supported currencies — checkout validates against this allowlist (USD base). */
object SupportedCurrencies {
    val ALL = setOf("USD", "EUR", "GBP", "BDT", "INR")
    fun requireValid(code: String) {
        require(code.uppercase() in ALL) { "Unsupported currency: $code. Supported: ${ALL.sorted().joinToString()}" }
    }
}
