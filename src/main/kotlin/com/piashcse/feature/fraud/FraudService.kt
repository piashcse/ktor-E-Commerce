package com.piashcse.feature.fraud

import com.piashcse.constants.AvsResult
import com.piashcse.constants.BlocklistType
import com.piashcse.constants.CvvResult
import com.piashcse.constants.FraudMetric
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RiskAction
import com.piashcse.constants.RiskDecision
import com.piashcse.database.entities.BlocklistEntryDAO
import com.piashcse.database.entities.BlocklistEntryTable
import com.piashcse.database.entities.FraudRuleDAO
import com.piashcse.database.entities.FraudRuleTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.PaymentRiskDAO
import com.piashcse.database.entities.PaymentTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.mapper.toPaymentResponse
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime

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

class FraudService {
    /**
     * Evaluates blocklists and active velocity rules. Must run inside a DB transaction.
     * Strength order: BLOCK > REVIEW > REQUIRE_3DS > ALLOW.
     */
    fun evaluate(context: FraudContext): RiskAssessment {
        val reasons = mutableListOf<String>()
        var score = 0
        var decision = RiskDecision.ALLOW
        var requires3ds = false

        val normalizedEmail = context.email?.lowercase()
        val blocklisted =
            BlocklistEntryDAO.find { BlocklistEntryTable.isActive eq true }
                .firstOrNull { entry ->
                    entry.isLive() &&
                        when (entry.type) {
                            BlocklistType.USER -> entry.value == context.userId
                            BlocklistType.EMAIL -> entry.value.lowercase() == normalizedEmail
                            BlocklistType.IP -> entry.value == context.ipAddress
                        }
                }
        if (blocklisted != null) {
            return RiskAssessment(
                decision = RiskDecision.BLOCK,
                score = 100,
                reasons = listOf("Blocklisted ${blocklisted.type.name.lowercase()}: ${blocklisted.value}"),
            )
        }

        val rules =
            FraudRuleDAO.find { FraudRuleTable.isActive eq true }
                .sortedByDescending { it.priority }
        rules.forEach { rule ->
            val value = metricValue(rule.metric, rule.windowMinutes, context)
            if (value > rule.threshold) {
                reasons += "${rule.name}: ${rule.metric} $value > ${rule.threshold.toPlainString()}"
                when (rule.action) {
                    RiskAction.BLOCK -> {
                        decision = RiskDecision.BLOCK
                        score = maxOf(score, 90)
                    }
                    RiskAction.REVIEW -> {
                        if (decision != RiskDecision.BLOCK) decision = RiskDecision.REVIEW
                        score = maxOf(score, 50)
                    }
                    RiskAction.REQUIRE_3DS -> {
                        requires3ds = true
                        score = maxOf(score, 25)
                    }
                    RiskAction.ALLOW -> Unit
                }
            }
        }
        return RiskAssessment(decision, score.coerceAtMost(100), reasons, requires3ds)
    }

    /** Must run inside the payment transaction. */
    fun recordRisk(
        payment: PaymentDAO,
        assessment: RiskAssessment,
        avsStatus: AvsResult = assessment.avsStatus,
        cvvStatus: CvvResult = assessment.cvvStatus,
    ) {
        payment.riskScore = assessment.score
        payment.riskDecision = assessment.decision
        payment.threeDsEnforced = assessment.requires3ds
        payment.avsStatus = avsStatus
        payment.cvvStatus = cvvStatus
        PaymentRiskDAO.new {
            this.paymentId = payment.id
            this.score = assessment.score
            this.decision = assessment.decision
            this.reasons = assessment.reasons.joinToString("; ").takeIf { it.isNotBlank() }
            this.avsStatus = avsStatus
            this.cvvStatus = cvvStatus
            this.requires3ds = assessment.requires3ds
        }
    }

    suspend fun recordRiskResult(
        paymentId: String,
        request: RiskResultRequest,
    ) {
        query {
            val payment = PaymentDAO.findById(paymentId) ?: paymentId.throwNotFound("Payment")
            PaymentRiskDAO.new {
                this.paymentId = payment.id
                this.score = payment.riskScore
                this.decision = payment.riskDecision
                this.reasons = "AVS/CVV updated by admin"
                this.avsStatus = request.avsStatus
                this.cvvStatus = request.cvvStatus
                this.requires3ds = payment.threeDsEnforced
            }
            payment.avsStatus = request.avsStatus
            payment.cvvStatus = request.cvvStatus
        }
    }

    suspend fun listRules(): List<FraudRuleResponse> = query { FraudRuleDAO.all().map { it.toResponse() } }

    suspend fun createRule(request: FraudRuleRequest): FraudRuleResponse =
        query {
            FraudRuleDAO.new {
                name = request.name
                metric = request.metric
                windowMinutes = request.windowMinutes
                threshold = request.threshold
                action = request.action
                priority = request.priority
                isActive = request.isActive
            }.toResponse()
        }

    suspend fun updateRule(
        id: String,
        request: FraudRuleRequest,
    ): FraudRuleResponse =
        query {
            val rule = FraudRuleDAO.findById(id) ?: id.throwNotFound("Fraud rule")
            rule.apply {
                name = request.name
                metric = request.metric
                windowMinutes = request.windowMinutes
                threshold = request.threshold
                action = request.action
                priority = request.priority
                isActive = request.isActive
            }.toResponse()
        }

    suspend fun deleteRule(id: String): Boolean =
        query {
            val rule = FraudRuleDAO.findById(id) ?: return@query false
            rule.delete()
            true
        }

    suspend fun listBlocklist(): List<BlocklistResponse> = query { BlocklistEntryDAO.all().map { it.toResponse() } }

    suspend fun createBlocklistEntry(request: BlocklistRequest): BlocklistResponse =
        query {
            val existing =
                BlocklistEntryDAO.find {
                    (BlocklistEntryTable.type eq request.type) and (BlocklistEntryTable.value eq request.value)
                }.firstOrNull()
            if (existing != null) throw ValidationException("Blocklist entry already exists")
            BlocklistEntryDAO.new {
                type = request.type
                value = request.value
                reason = request.reason
                isActive = request.isActive
                expiresAt = parseDate(request.expiresAt)
            }.toResponse()
        }

    suspend fun deleteBlocklistEntry(id: String): Boolean =
        query {
            val entry = BlocklistEntryDAO.findById(id) ?: return@query false
            entry.delete()
            true
        }

    suspend fun listHighRisk(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<com.piashcse.model.response.PaymentResponse> =
        query {
            PaymentTable.selectAll()
                .andWhere { PaymentTable.riskDecision inList listOf(RiskDecision.REVIEW, RiskDecision.BLOCK) }
                .toPaginatedResponse(limit, offset) {
                    PaymentDAO.wrapRow(it).toPaymentResponse()
                }
        }

    private fun metricValue(
        metric: FraudMetric,
        windowMinutes: Int,
        context: FraudContext,
    ): BigDecimal {
        val since = currentUtc().minusMinutes(windowMinutes.toLong())
        return when (metric) {
            FraudMetric.ORDER_COUNT ->
                BigDecimal(
                    OrderDAO.find {
                        (OrderTable.userId eq context.userId.entityID(com.piashcse.database.entities.UserTable)) and
                            (OrderTable.createdAt greaterEq since)
                    }.count(),
                )
            FraudMetric.ORDER_AMOUNT ->
                OrderDAO.find {
                    (OrderTable.userId eq context.userId.entityID(com.piashcse.database.entities.UserTable)) and
                        (OrderTable.createdAt greaterEq since) and
                        (OrderTable.status neq OrderStatus.CANCELED)
                }.fold(BigDecimal.ZERO) { acc, order ->
                    val usd =
                        if (order.fxRate > BigDecimal.ZERO) {
                            order.total.divide(order.fxRate, 2, java.math.RoundingMode.HALF_UP)
                        } else {
                            order.total
                        }
                    acc.add(usd)
                }
            FraudMetric.PAYMENT_ATTEMPTS ->
                BigDecimal(
                    PaymentDAO.find {
                        (PaymentTable.userId eq context.userId.entityID(com.piashcse.database.entities.UserTable)) and
                            (PaymentTable.createdAt greaterEq since) and
                            (PaymentTable.status neq PaymentStatus.REFUNDED)
                    }.count(),
                )
        }
    }

    private fun parseDate(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) return null
        return runCatching { LocalDateTime.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).atZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime() }
            .getOrElse { throw ValidationException("Invalid expiresAt: $value") }
    }

    private fun FraudRuleDAO.toResponse() =
        FraudRuleResponse(
            id = id.value,
            name = name,
            metric = metric,
            windowMinutes = windowMinutes,
            threshold = Money.str(threshold),
            action = action,
            priority = priority,
            isActive = isActive,
        )

    private fun BlocklistEntryDAO.toResponse() =
        BlocklistResponse(
            id = id.value,
            type = type,
            value = value,
            reason = reason,
            isActive = isActive,
            expiresAt = expiresAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
        )
}
