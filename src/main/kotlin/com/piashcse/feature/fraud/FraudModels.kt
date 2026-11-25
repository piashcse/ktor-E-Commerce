package com.piashcse.feature.fraud

import com.piashcse.constants.AvsResult
import com.piashcse.constants.BlocklistType
import com.piashcse.constants.CvvResult
import com.piashcse.constants.FraudMetric
import com.piashcse.constants.RiskAction
import com.piashcse.constants.RiskDecision
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class FraudRuleRequest(
    val name: String,
    val metric: FraudMetric,
    val windowMinutes: Int = 60,
    @Contextual val threshold: BigDecimal,
    val action: RiskAction = RiskAction.REVIEW,
    val priority: Int = 0,
    val isActive: Boolean = true,
) {
    init {
        val violations =
            buildList {
                if (name.isBlank()) add(FieldViolation("name", "Rule name is required"))
                if (windowMinutes <= 0) add(FieldViolation("windowMinutes", "Window must be positive"))
                if (threshold <= BigDecimal.ZERO) add(FieldViolation("threshold", "Threshold must be positive"))
            }
        if (violations.isNotEmpty()) throw FieldValidationException(violations)
    }
}

@Serializable
data class FraudRuleResponse(
    val id: String,
    val name: String,
    val metric: FraudMetric,
    val windowMinutes: Int,
    val threshold: String,
    val action: RiskAction,
    val priority: Int,
    val isActive: Boolean,
)

@Serializable
data class BlocklistRequest(
    val type: BlocklistType,
    val value: String,
    val reason: String? = null,
    val expiresAt: String? = null,
    val isActive: Boolean = true,
) {
    init {
        if (value.isBlank()) throw FieldValidationException(listOf(FieldViolation("value", "Value is required")))
    }
}

@Serializable
data class BlocklistResponse(
    val id: String,
    val type: BlocklistType,
    val value: String,
    val reason: String?,
    val isActive: Boolean,
    val expiresAt: String?,
    val createdAt: String?,
)

@Serializable
data class RiskResultRequest(
    val avsStatus: AvsResult = AvsResult.UNKNOWN,
    val cvvStatus: CvvResult = CvvResult.UNKNOWN,
)

data class FraudContext(
    val userId: String,
    val email: String?,
    val ipAddress: String?,
    val amount: BigDecimal,
)

data class RiskAssessment(
    val decision: RiskDecision = RiskDecision.ALLOW,
    val score: Int = 0,
    val reasons: List<String> = emptyList(),
    val requires3ds: Boolean = false,
    val avsStatus: AvsResult = AvsResult.UNKNOWN,
    val cvvStatus: CvvResult = CvvResult.UNKNOWN,
) {
    val blocked: Boolean get() = decision == RiskDecision.BLOCK
}

@Serializable
data class ChargebackEvidenceRequest(val evidenceUrl: String)
