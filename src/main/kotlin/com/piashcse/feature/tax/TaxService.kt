package com.piashcse.feature.tax

import com.piashcse.database.entities.TaxClassDAO
import com.piashcse.database.entities.TaxClassTable
import com.piashcse.database.entities.TaxExemptionDAO
import com.piashcse.database.entities.TaxExemptionTable
import com.piashcse.database.entities.TaxRuleDAO
import com.piashcse.database.entities.TaxRuleTable
import com.piashcse.database.entities.TaxZoneDAO
import com.piashcse.database.entities.TaxZoneTable
import com.piashcse.database.entities.isLive
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.valiktor.functions.isNotBlank
import java.math.BigDecimal
import java.math.RoundingMode

class TaxService {
    fun isExempt(
        userId: String?,
        businessTaxId: String?,
        country: String?,
    ): Boolean {
        // Exemptions are account-bound. A client-supplied business tax ID must
        // never grant exemption on its own: otherwise anyone who knows a
        // reseller's ID can check out tax-free.
        if (userId == null) return false
        val normalized = country?.uppercase()
        return TaxExemptionDAO.find { TaxExemptionTable.isActive eq true }
            .firstOrNull { exemption ->
                exemption.isLive() &&
                    (exemption.country == null || exemption.country == normalized) &&
                    exemption.userId?.value == userId &&
                    (businessTaxId == null || exemption.businessTaxId == null || exemption.businessTaxId == businessTaxId)
            } != null
    }

    fun resolve(
        country: String,
        region: String?,
        categoryId: String?,
        taxClassId: String?,
        userId: String? = null,
        businessTaxId: String? = null,
    ): TaxQuote {
        val normalized = country.uppercase()
        if (isExempt(userId, businessTaxId, normalized)) {
            return TaxQuote(rate = BigDecimal.ZERO, inclusive = false, taxClassId = taxClassId, exempt = true)
        }

        val matchingZones =
            TaxZoneDAO.find {
                (TaxZoneTable.country eq normalized) and (TaxZoneTable.isActive eq true)
            }.filter { it.region == null || it.region.equals(region, ignoreCase = true) }
        // A region-specific zone outranks a country-wide fallback regardless of
        // priority; priority only breaks ties within the same specificity.
        val zone =
            (
                matchingZones.filter { it.region != null }.maxByOrNull { it.priority }
                    ?: matchingZones.filter { it.region == null }.maxByOrNull { it.priority }
            )

        if (zone != null && !zone.isNexus) {
            return TaxQuote(rate = BigDecimal.ZERO, inclusive = false, taxClassId = taxClassId)
        }

        val classId = taxClassId?.takeIf { it.isNotBlank() }
        val category = categoryId?.takeIf { it.isNotBlank() }
        val candidates =
            TaxRuleDAO.find {
                (TaxRuleTable.country eq normalized) and (TaxRuleTable.isActive eq true)
            }.filter { rule ->
                val zoneOk = rule.zoneId == null || rule.zoneId?.value == zone?.id?.value
                val classOk = rule.taxClassId == null || rule.taxClassId?.value == classId
                val categoryOk = rule.categoryId == null || rule.categoryId?.value == category
                zoneOk && classOk && categoryOk
            }
        val best =
            candidates.sortedWith(
                compareByDescending<TaxRuleDAO> { specificity(it) }
                    .thenByDescending { it.priority }
                    .thenBy { it.createdAt },
            ).firstOrNull()
        if (best != null) {
            return TaxQuote(best.rate, best.isInclusive, best.taxClassId?.value ?: classId)
        }

        val taxClass =
            classId?.let { id ->
                TaxClassDAO.find { TaxClassTable.id eq id.entityID(TaxClassTable) }
                    .firstOrNull()
                    ?.takeIf { it.isActive }
            }
        if (taxClass != null) {
            return TaxQuote(taxClass.rate, taxClass.isInclusive, taxClass.id.value)
        }

        // No nexus/rule configured means no tax. A hardcoded 5% fallback
        // silently taxed destinations where the merchant had no obligation.
        return TaxQuote(BigDecimal.ZERO.setScale(2), false, classId)
    }

    fun apply(
        amount: BigDecimal,
        quote: TaxQuote,
    ): TaxAmounts = applyTax(amount, quote)

    companion object {
        /**
         * Pure tax calculation shared by [apply] and preview/quote builders.
         * Keep this DB-free so it can be unit-tested without a database.
         */
        fun applyTax(
            amount: BigDecimal,
            quote: TaxQuote,
        ): TaxAmounts {
            if (quote.exempt || quote.rate <= BigDecimal.ZERO || amount <= BigDecimal.ZERO) {
                return TaxAmounts(amount, BigDecimal.ZERO.setScale(2), amount)
            }
            return if (quote.inclusive) {
                val net = amount.divide(BigDecimal.ONE.add(quote.rate), 2, RoundingMode.HALF_UP)
                val tax = amount.subtract(net).max(BigDecimal.ZERO)
                TaxAmounts(net, Money.scale2(tax), amount)
            } else {
                val tax = Money.scale2(amount.multiply(quote.rate))
                TaxAmounts(amount, tax, amount.add(tax))
            }
        }
    }

    private fun specificity(rule: TaxRuleDAO): Int =
        (if (rule.zoneId != null) 4 else 0) +
            (if (rule.taxClassId != null) 2 else 0) +
            (if (rule.categoryId != null) 1 else 0)

    suspend fun list(): List<TaxRuleResponse> = TaxAdminService.list()

    suspend fun upsert(request: TaxRuleRequest): TaxRuleResponse = TaxAdminService.upsert(request)

    suspend fun delete(id: String): Boolean = TaxAdminService.delete(id)

    suspend fun listClasses(): List<TaxClassResponse> = TaxAdminService.listClasses()

    suspend fun createClass(request: TaxClassRequest): TaxClassResponse = TaxAdminService.createClass(request)

    suspend fun updateClass(
        id: String,
        request: TaxClassRequest,
    ): TaxClassResponse = TaxAdminService.updateClass(id, request)

    suspend fun deleteClass(id: String): Boolean = TaxAdminService.deleteClass(id)

    suspend fun listZones(): List<TaxZoneResponse> = TaxAdminService.listZones()

    suspend fun createZone(request: TaxZoneRequest): TaxZoneResponse = TaxAdminService.createZone(request)

    suspend fun updateZone(
        id: String,
        request: TaxZoneRequest,
    ): TaxZoneResponse = TaxAdminService.updateZone(id, request)

    suspend fun deleteZone(id: String): Boolean = TaxAdminService.deleteZone(id)

    suspend fun listExemptions(): List<TaxExemptionResponse> = TaxAdminService.listExemptions()

    suspend fun createExemption(request: TaxExemptionRequest): TaxExemptionResponse = TaxAdminService.createExemption(request)

    suspend fun deleteExemption(id: String): Boolean = TaxAdminService.deleteExemption(id)

    suspend fun assignProductTaxClass(
        productId: String,
        taxClassId: String?,
    ) = TaxAdminService.assignProductTaxClass(productId, taxClassId)
}
