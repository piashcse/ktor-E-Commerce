package com.piashcse.database.entities

import com.piashcse.constants.AvsResult
import com.piashcse.constants.CvvResult
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RiskDecision
import com.piashcse.constants.ScaStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object PaymentTable : BaseIdTable("payment") {
    val orderId = reference("order_id", OrderTable.id)
    val userId = reference("user_id", UserTable.id)
    val amount = decimal("amount", 10, 2)
    val currency = varchar("currency", 3).default("USD")
    val status = enumerationByName("status", 30, PaymentStatus::class).clientDefault { PaymentStatus.PENDING }
    val paymentMethod = enumerationByName("payment_method", 50, PaymentMethod::class)
    val transactionId = varchar("transaction_id", 100).nullable().uniqueIndex()

    /** Client-supplied idempotency key, scoped per user by the DB partial index. */
    val idempotencyKey = varchar("idempotency_key", 100).nullable()

    /** Processor that owns the charge. MANUAL is the COD/admin-confirmed gateway. */
    val provider = enumerationByName("provider", 30, PaymentProvider::class).default(PaymentProvider.MANUAL)

    /** Provider object id (for Stripe: the PaymentIntent id). */
    val providerReference = varchar("provider_reference", 150).nullable().uniqueIndex()

    /** Client-side secret used to confirm the intent / trigger 3DS. */
    val clientSecret = varchar("client_secret", 255).nullable()

    /** Redirect URL for SCA/3DS when the provider requires a browser step. */
    val nextActionUrl = varchar("next_action_url", 1000).nullable()
    val scaStatus = enumerationByName("sca_status", 30, ScaStatus::class).default(ScaStatus.NOT_REQUIRED)
    val providerStatus = varchar("provider_status", 50).nullable()

    val riskScore = integer("risk_score").default(0)
    val riskDecision = enumerationByName("risk_decision", 20, RiskDecision::class).default(RiskDecision.ALLOW)
    val avsStatus = enumerationByName("avs_status", 20, AvsResult::class).default(AvsResult.UNKNOWN)
    val cvvStatus = enumerationByName("cvv_status", 20, CvvResult::class).default(CvvResult.UNKNOWN)
    val threeDsEnforced = bool("three_ds_enforced").default(false)
}

class PaymentDAO(id: EntityID<String>) : BaseEntity(id, PaymentTable) {
    companion object : BaseEntityClass<PaymentDAO>(PaymentTable, PaymentDAO::class.java)

    var paymentId by PaymentTable.id
    var orderId by PaymentTable.orderId
    var userId by PaymentTable.userId
    var amount by PaymentTable.amount
    var currency by PaymentTable.currency
    var status by PaymentTable.status
    var paymentMethod by PaymentTable.paymentMethod
    var transactionId by PaymentTable.transactionId
    var idempotencyKey by PaymentTable.idempotencyKey
    var provider by PaymentTable.provider
    var providerReference by PaymentTable.providerReference
    var clientSecret by PaymentTable.clientSecret
    var nextActionUrl by PaymentTable.nextActionUrl
    var scaStatus by PaymentTable.scaStatus
    var providerStatus by PaymentTable.providerStatus
    var riskScore by PaymentTable.riskScore
    var riskDecision by PaymentTable.riskDecision
    var avsStatus by PaymentTable.avsStatus
    var cvvStatus by PaymentTable.cvvStatus
    var threeDsEnforced by PaymentTable.threeDsEnforced
}
