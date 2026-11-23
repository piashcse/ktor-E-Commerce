package com.piashcse.feature.tax

import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.valiktor.functions.isBetween
import org.valiktor.functions.isNotBlank
import org.valiktor.functions.isNotNull
import org.valiktor.validate
import java.math.BigDecimal

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
