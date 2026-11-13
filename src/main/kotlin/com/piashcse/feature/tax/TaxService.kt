package com.piashcse.feature.tax

import com.piashcse.constants.AppConstants
import com.piashcse.database.entities.ProductCategoryTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.TaxClassDAO
import com.piashcse.database.entities.TaxClassTable
import com.piashcse.database.entities.TaxExemptionDAO
import com.piashcse.database.entities.TaxExemptionTable
import com.piashcse.database.entities.TaxRuleDAO
import com.piashcse.database.entities.TaxRuleTable
import com.piashcse.database.entities.TaxZoneDAO
import com.piashcse.database.entities.TaxZoneTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.isLive
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.valiktor.functions.isBetween
import org.valiktor.functions.isNotBlank
import org.valiktor.functions.isNotNull
import org.valiktor.validate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Serializable
data class TaxRuleRequest(
    val country: String,
    val categoryId: String? = null,
    val zoneId: String? = null,
    val taxClassId: String? = null,
    @Contextual val rate: BigDecimal,
    val isInclusive: Boolean = false,
    val priority: Int = 0,
    val isActive: Boolean = true,
) {
    init {
        validate(this) {
            validate(TaxRuleRequest::country).isNotNull().isNotBlank()
            validate(TaxRuleRequest::rate).isNotNull().isBetween(BigDecimal.ZERO, BigDecimal.ONE)
        }
    }
}

@Serializable
data class TaxRuleResponse(
    val id: String,
    val country: String,
    val categoryId: String?,
    val zoneId: String?,
    val taxClassId: String?,
    val rate: String,
    val isInclusive: Boolean,
    val priority: Int,
    val isActive: Boolean,
)

@Serializable
data class TaxClassRequest(
    val name: String,
    val code: String,
    @Contextual val rate: BigDecimal,
    val isInclusive: Boolean = false,
    val isActive: Boolean = true,
) {
    init {
        if (name.isBlank()) throw FieldValidationException(listOf(FieldViolation("name", "Tax class name is required")))
        if (code.isBlank()) throw FieldValidationException(listOf(FieldViolation("code", "Tax class code is required")))
        if (rate < BigDecimal.ZERO || rate > BigDecimal.ONE) {
            throw FieldValidationException(listOf(FieldViolation("rate", "Tax class rate must be between 0 and 1")))
        }
    }
}

@Serializable
data class TaxClassResponse(
    val id: String,
    val name: String,
    val code: String,
    val rate: String,
    val isInclusive: Boolean,
    val isActive: Boolean,
)

@Serializable
data class TaxZoneRequest(
    val name: String,
    val country: String,
    val region: String? = null,
    val isNexus: Boolean = true,
    val priority: Int = 0,
    val isActive: Boolean = true,
)

@Serializable
data class TaxZoneResponse(
    val id: String,
    val name: String,
    val country: String,
    val region: String?,
    val isNexus: Boolean,
    val priority: Int,
    val isActive: Boolean,
)

@Serializable
data class TaxExemptionRequest(
    val userId: String? = null,
    val businessTaxId: String? = null,
    val country: String? = null,
    val reason: String,
    val certificateUrl: String? = null,
    val expiresAt: String? = null,
    val isActive: Boolean = true,
) {
    init {
        if (userId.isNullOrBlank() && businessTaxId.isNullOrBlank()) {
            throw FieldValidationException(
                listOf(FieldViolation("userId", "A customer or business tax id is required")),
            )
        }
        if (reason.isBlank()) throw FieldValidationException(listOf(FieldViolation("reason", "Reason is required")))
    }
}

@Serializable
data class TaxExemptionResponse(
    val id: String,
    val userId: String?,
    val businessTaxId: String?,
    val country: String?,
    val reason: String,
    val certificateUrl: String?,
    val isActive: Boolean,
    val expiresAt: String?,
    val createdAt: String?,
)

@Serializable
data class ProductTaxClassRequest(val taxClassId: String? = null)

/** Resolved tax treatment for one line. */
data class TaxQuote(
    val rate: BigDecimal,
    val inclusive: Boolean,
    val taxClassId: String? = null,
    val exempt: Boolean = false,
)

/** Monetary split for one line; [net] + [tax] = [gross] for both pricing modes. */
data class TaxAmounts(
    val net: BigDecimal,
    val tax: BigDecimal,
    val gross: BigDecimal,
)

/**
 * Resolves tax per line with this precedence:
 * exemption → nexus zone → most specific active rule → tax-class default → application default.
 *
 * `rate` values are decimal fractions (0.15 = 15%). Inclusive rules treat the input amount as
 * gross; exclusive rules treat it as net.
 */
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

        val zone =
            TaxZoneDAO.find {
                (TaxZoneTable.country eq normalized) and (TaxZoneTable.isActive eq true)
            }.filter { it.region == null || it.region.equals(region, ignoreCase = true) }
                .maxByOrNull { it.priority }

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

        return TaxQuote(Money.of(AppConstants.DEFAULT_TAX_PERCENTAGE), false, classId)
    }

    fun apply(
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

    private fun specificity(rule: TaxRuleDAO): Int =
        (if (rule.zoneId != null) 4 else 0) +
            (if (rule.taxClassId != null) 2 else 0) +
            (if (rule.categoryId != null) 1 else 0)

    suspend fun list(): List<TaxRuleResponse> =
        query {
            TaxRuleTable.selectAll()
                .orderBy(TaxRuleTable.country to org.jetbrains.exposed.v1.core.SortOrder.ASC)
                .map { TaxRuleDAO.wrapRow(it).toResponse() }
        }

    suspend fun upsert(request: TaxRuleRequest): TaxRuleResponse =
        query {
            val country = request.country.uppercase()
            val categoryId = request.categoryId?.takeIf { it.isNotBlank() }
            val zoneId = request.zoneId?.takeIf { it.isNotBlank() }
            val taxClassId = request.taxClassId?.takeIf { it.isNotBlank() }
            requireCategory(categoryId)
            requireZone(zoneId)
            requireTaxClass(taxClassId)

            val existing =
                TaxRuleDAO.find { TaxRuleTable.country eq country }
                    .firstOrNull {
                        it.categoryId?.value == categoryId &&
                            it.zoneId?.value == zoneId &&
                            it.taxClassId?.value == taxClassId
                    }
            val dao =
                existing?.apply {
                    rate = request.rate
                    isInclusive = request.isInclusive
                    priority = request.priority
                    isActive = request.isActive
                } ?: TaxRuleDAO.new {
                    this.country = country
                    this.categoryId = categoryId?.entityID(ProductCategoryTable)
                    this.zoneId = zoneId?.entityID(TaxZoneTable)
                    this.taxClassId = taxClassId?.entityID(TaxClassTable)
                    this.rate = request.rate
                    this.isInclusive = request.isInclusive
                    this.priority = request.priority
                    this.isActive = request.isActive
                }
            dao.toResponse()
        }

    suspend fun delete(id: String): Boolean =
        query {
            val rule = TaxRuleDAO.findById(id) ?: return@query false
            rule.delete()
            true
        }

    suspend fun listClasses(): List<TaxClassResponse> =
        query { TaxClassDAO.all().orderBy(TaxClassTable.name to SortOrder.ASC).map { it.toResponse() } }

    suspend fun createClass(request: TaxClassRequest): TaxClassResponse =
        query {
            if (TaxClassDAO.find { TaxClassTable.code eq request.code }.any()) {
                throw ValidationException("Tax class code already exists")
            }
            TaxClassDAO.new {
                name = request.name
                code = request.code
                rate = request.rate
                isInclusive = request.isInclusive
                isActive = request.isActive
            }.toResponse()
        }

    suspend fun updateClass(
        id: String,
        request: TaxClassRequest,
    ): TaxClassResponse =
        query {
            val taxClass = TaxClassDAO.findById(id) ?: id.throwNotFound("Tax class")
            taxClass.apply {
                name = request.name
                code = request.code
                rate = request.rate
                isInclusive = request.isInclusive
                isActive = request.isActive
            }.toResponse()
        }

    suspend fun deleteClass(id: String): Boolean =
        query {
            val taxClass = TaxClassDAO.findById(id) ?: return@query false
            taxClass.delete()
            true
        }

    suspend fun listZones(): List<TaxZoneResponse> =
        query { TaxZoneDAO.all().orderBy(TaxZoneTable.country to SortOrder.ASC).map { it.toResponse() } }

    suspend fun createZone(request: TaxZoneRequest): TaxZoneResponse =
        query {
            TaxZoneDAO.new {
                name = request.name
                country = request.country.uppercase()
                region = request.region?.takeIf { it.isNotBlank() }
                isNexus = request.isNexus
                priority = request.priority
                isActive = request.isActive
            }.toResponse()
        }

    suspend fun updateZone(
        id: String,
        request: TaxZoneRequest,
    ): TaxZoneResponse =
        query {
            val zone = TaxZoneDAO.findById(id) ?: id.throwNotFound("Tax zone")
            zone.apply {
                name = request.name
                country = request.country.uppercase()
                region = request.region?.takeIf { it.isNotBlank() }
                isNexus = request.isNexus
                priority = request.priority
                isActive = request.isActive
            }.toResponse()
        }

    suspend fun deleteZone(id: String): Boolean =
        query {
            val zone = TaxZoneDAO.findById(id) ?: return@query false
            zone.delete()
            true
        }

    suspend fun listExemptions(): List<TaxExemptionResponse> = query { TaxExemptionDAO.all().map { it.toResponse() } }

    suspend fun createExemption(request: TaxExemptionRequest): TaxExemptionResponse =
        query {
            request.userId?.takeIf { it.isNotBlank() }?.let {
                if (UserTable.selectAll().where { UserTable.id eq it.entityID(UserTable) }.count() == 0L) {
                    throw ValidationException("User not found")
                }
            }
            TaxExemptionDAO.new {
                userId = request.userId?.takeIf { it.isNotBlank() }?.entityID(UserTable)
                businessTaxId = request.businessTaxId?.takeIf { it.isNotBlank() }
                country = request.country?.takeIf { it.isNotBlank() }?.uppercase()
                reason = request.reason
                certificateUrl = request.certificateUrl
                isActive = request.isActive
                expiresAt = parseDate(request.expiresAt)
            }.toResponse()
        }

    suspend fun deleteExemption(id: String): Boolean =
        query {
            val exemption = TaxExemptionDAO.findById(id) ?: return@query false
            exemption.delete()
            true
        }

    suspend fun assignProductTaxClass(
        productId: String,
        taxClassId: String?,
    ) {
        query {
            val product = ProductDAO.findById(productId) ?: productId.throwNotFound("Product")
            val normalizedClass = taxClassId?.takeIf { it.isNotBlank() }
            requireTaxClass(normalizedClass)
            product.taxClassId = normalizedClass?.entityID(TaxClassTable)
        }
    }

    private fun requireCategory(categoryId: String?) {
        if (categoryId == null) return
        if (ProductCategoryTable.selectAll().where {
                ProductCategoryTable.id eq categoryId.entityID(ProductCategoryTable)
            }.count() == 0L
        ) {
            throw ValidationException("Category not found")
        }
    }

    private fun requireZone(zoneId: String?) {
        if (zoneId == null) return
        if (!TaxZoneDAO.find { TaxZoneTable.id eq zoneId.entityID(TaxZoneTable) }.any()) {
            throw ValidationException("Tax zone not found")
        }
    }

    private fun requireTaxClass(taxClassId: String?) {
        if (taxClassId == null) return
        if (!TaxClassDAO.find { TaxClassTable.id eq taxClassId.entityID(TaxClassTable) }.any()) {
            throw ValidationException("Tax class not found")
        }
    }

    private fun parseDate(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) return null
        return runCatching { LocalDateTime.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).toLocalDateTime() }
            .getOrElse { throw ValidationException("Invalid expiresAt: $value") }
    }

    private fun TaxRuleDAO.toResponse() =
        TaxRuleResponse(
            id = id.value,
            country = country,
            categoryId = categoryId?.value,
            zoneId = zoneId?.value,
            taxClassId = taxClassId?.value,
            rate = rate.toPlainString(),
            isInclusive = isInclusive,
            priority = priority,
            isActive = isActive,
        )

    private fun TaxClassDAO.toResponse() =
        TaxClassResponse(
            id = id.value,
            name = name,
            code = code,
            rate = rate.toPlainString(),
            isInclusive = isInclusive,
            isActive = isActive,
        )

    private fun TaxZoneDAO.toResponse() =
        TaxZoneResponse(
            id = id.value,
            name = name,
            country = country,
            region = region,
            isNexus = isNexus,
            priority = priority,
            isActive = isActive,
        )

    private fun TaxExemptionDAO.toResponse() =
        TaxExemptionResponse(
            id = id.value,
            userId = userId?.value,
            businessTaxId = businessTaxId,
            country = country,
            reason = reason,
            certificateUrl = certificateUrl,
            isActive = isActive,
            expiresAt = expiresAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
        )
}
