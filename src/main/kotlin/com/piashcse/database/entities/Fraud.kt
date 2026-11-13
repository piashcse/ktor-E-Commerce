package com.piashcse.database.entities

import com.piashcse.constants.AvsResult
import com.piashcse.constants.BlocklistType
import com.piashcse.constants.ChargebackStatus
import com.piashcse.constants.CvvResult
import com.piashcse.constants.FraudMetric
import com.piashcse.constants.RiskAction
import com.piashcse.constants.RiskDecision
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.database.entities.base.currentUtc
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object FraudRuleTable : BaseIdTable("fraud_rule") {
    val name = varchar("name", 150)
    val metric = enumerationByName("metric", 30, FraudMetric::class)
    val windowMinutes = integer("window_minutes").default(60)
    val threshold = decimal("threshold", 12, 2)
    val action = enumerationByName("action", 20, RiskAction::class).default(RiskAction.REVIEW)
    val priority = integer("priority").default(0)
    val isActive = bool("is_active").default(true)
}

class FraudRuleDAO(id: EntityID<String>) : BaseEntity(id, FraudRuleTable) {
    companion object : BaseEntityClass<FraudRuleDAO>(FraudRuleTable, FraudRuleDAO::class.java)

    var name by FraudRuleTable.name
    var metric by FraudRuleTable.metric
    var windowMinutes by FraudRuleTable.windowMinutes
    var threshold by FraudRuleTable.threshold
    var action by FraudRuleTable.action
    var priority by FraudRuleTable.priority
    var isActive by FraudRuleTable.isActive
}

object BlocklistEntryTable : BaseIdTable("blocklist_entry") {
    val type = enumerationByName("type", 20, BlocklistType::class)
    val value = varchar("value", 255)
    val reason = text("reason").nullable()
    val isActive = bool("is_active").default(true)
    val expiresAt = datetime("expires_at").nullable()

    init {
        uniqueIndex("blocklist_entry_type_value_unique", type, value)
    }
}

class BlocklistEntryDAO(id: EntityID<String>) : BaseEntity(id, BlocklistEntryTable) {
    companion object : BaseEntityClass<BlocklistEntryDAO>(BlocklistEntryTable, BlocklistEntryDAO::class.java)

    var type by BlocklistEntryTable.type
    var value by BlocklistEntryTable.value
    var reason by BlocklistEntryTable.reason
    var isActive by BlocklistEntryTable.isActive
    var expiresAt by BlocklistEntryTable.expiresAt

    fun isLive(): Boolean = isActive && (expiresAt?.isAfter(currentUtc()) ?: true)
}

/** Immutable decision history for each payment; also carries AVS/CVV results. */
object PaymentRiskTable : BaseIdTable("payment_risk") {
    val paymentId = reference("payment_id", PaymentTable.id).index()
    val score = integer("score").default(0)
    val decision = enumerationByName("decision", 20, RiskDecision::class).default(RiskDecision.ALLOW).index()
    val reasons = text("reasons").nullable()
    val avsStatus = enumerationByName("avs_status", 20, AvsResult::class).default(AvsResult.UNKNOWN)
    val cvvStatus = enumerationByName("cvv_status", 20, CvvResult::class).default(CvvResult.UNKNOWN)
    val requires3ds = bool("requires_3ds").default(false)
    val evaluatedAt = datetime("evaluated_at").clientDefault { currentUtc() }
}

class PaymentRiskDAO(id: EntityID<String>) : BaseEntity(id, PaymentRiskTable) {
    companion object : BaseEntityClass<PaymentRiskDAO>(PaymentRiskTable, PaymentRiskDAO::class.java)

    var paymentId by PaymentRiskTable.paymentId
    var score by PaymentRiskTable.score
    var decision by PaymentRiskTable.decision
    var reasons by PaymentRiskTable.reasons
    var avsStatus by PaymentRiskTable.avsStatus
    var cvvStatus by PaymentRiskTable.cvvStatus
    var requires3ds by PaymentRiskTable.requires3ds
    var evaluatedAt by PaymentRiskTable.evaluatedAt
}

object ChargebackTable : BaseIdTable("chargeback") {
    val paymentId = reference("payment_id", PaymentTable.id).index()
    val orderId = reference("order_id", OrderTable.id).index()
    val providerDisputeId = varchar("provider_dispute_id", 150).nullable()
    val amount = decimal("amount", 10, 2)
    val currency = varchar("currency", 3).default("USD")
    val status =
        enumerationByName("status", 30, ChargebackStatus::class)
            .default(ChargebackStatus.OPEN)
            .index()
    val reason = text("reason").nullable()
    val outcome = text("outcome").nullable()
    val evidenceDueAt = datetime("evidence_due_at").nullable()
    val openedAt = datetime("opened_at").clientDefault { currentUtc() }
    val resolvedAt = datetime("resolved_at").nullable()
}

class ChargebackDAO(id: EntityID<String>) : BaseEntity(id, ChargebackTable) {
    companion object : BaseEntityClass<ChargebackDAO>(ChargebackTable, ChargebackDAO::class.java)

    var paymentId by ChargebackTable.paymentId
    var orderId by ChargebackTable.orderId
    var providerDisputeId by ChargebackTable.providerDisputeId
    var amount by ChargebackTable.amount
    var currency by ChargebackTable.currency
    var status by ChargebackTable.status
    var reason by ChargebackTable.reason
    var outcome by ChargebackTable.outcome
    var evidenceDueAt by ChargebackTable.evidenceDueAt
    var openedAt by ChargebackTable.openedAt
    var resolvedAt by ChargebackTable.resolvedAt
}

fun ChargebackDAO.isResolved(): Boolean = status in listOf(ChargebackStatus.WON, ChargebackStatus.LOST, ChargebackStatus.CLOSED)
