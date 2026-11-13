package com.piashcse.database.entities

import com.piashcse.constants.GiftCardStatus
import com.piashcse.constants.ReferralStatus
import com.piashcse.constants.WalletEntryType
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.database.entities.base.currentUtc
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal

object WalletAccountTable : BaseIdTable("wallet_account") {
    val userId = reference("user_id", UserTable.id).uniqueIndex()
    val creditBalance = decimal("credit_balance", 12, 2).default(BigDecimal.ZERO)
    val pointsBalance = integer("points_balance").default(0)
    val lifetimePoints = integer("lifetime_points").default(0)
    val referralCode = varchar("referral_code", 20).uniqueIndex()
}

class WalletAccountDAO(id: EntityID<String>) : BaseEntity(id, WalletAccountTable) {
    companion object : BaseEntityClass<WalletAccountDAO>(WalletAccountTable, WalletAccountDAO::class.java)

    var userId by WalletAccountTable.userId
    var creditBalance by WalletAccountTable.creditBalance
    var pointsBalance by WalletAccountTable.pointsBalance
    var lifetimePoints by WalletAccountTable.lifetimePoints
    var referralCode by WalletAccountTable.referralCode
}

object WalletLedgerTable : BaseIdTable("wallet_ledger") {
    val userId = reference("user_id", UserTable.id).index()
    val type = enumerationByName("type", 30, WalletEntryType::class)
    val pointsDelta = integer("points_delta").default(0)
    val creditDelta = decimal("credit_delta", 12, 2).default(BigDecimal.ZERO)
    val referenceType = varchar("reference_type", 50).nullable()
    val referenceId = varchar("reference_id", 100).nullable()
    val note = text("note").nullable()
}

class WalletLedgerDAO(id: EntityID<String>) : BaseEntity(id, WalletLedgerTable) {
    companion object : BaseEntityClass<WalletLedgerDAO>(WalletLedgerTable, WalletLedgerDAO::class.java)

    var userId by WalletLedgerTable.userId
    var type by WalletLedgerTable.type
    var pointsDelta by WalletLedgerTable.pointsDelta
    var creditDelta by WalletLedgerTable.creditDelta
    var referenceType by WalletLedgerTable.referenceType
    var referenceId by WalletLedgerTable.referenceId
    var note by WalletLedgerTable.note
}

object ReferralTable : BaseIdTable("referral") {
    val referrerUserId = reference("referrer_user_id", UserTable.id).index()
    val refereeUserId = reference("referee_user_id", UserTable.id).uniqueIndex()
    val code = varchar("code", 20)
    val status = enumerationByName("status", 20, ReferralStatus::class).default(ReferralStatus.PENDING)
    val rewardPoints = integer("reward_points").default(0)
    val rewardCredit = decimal("reward_credit", 12, 2).default(BigDecimal.ZERO)
    val completedAt = datetime("completed_at").nullable()
}

class ReferralDAO(id: EntityID<String>) : BaseEntity(id, ReferralTable) {
    companion object : BaseEntityClass<ReferralDAO>(ReferralTable, ReferralDAO::class.java)

    var referrerUserId by ReferralTable.referrerUserId
    var refereeUserId by ReferralTable.refereeUserId
    var code by ReferralTable.code
    var status by ReferralTable.status
    var rewardPoints by ReferralTable.rewardPoints
    var rewardCredit by ReferralTable.rewardCredit
    var completedAt by ReferralTable.completedAt
}

object GiftCardTable : BaseIdTable("gift_card") {
    val code = varchar("code", 50).uniqueIndex()
    val initialBalance = decimal("initial_balance", 12, 2)
    val balance = decimal("balance", 12, 2)
    val currency = varchar("currency", 3).default("USD")
    val status = enumerationByName("status", 20, GiftCardStatus::class).default(GiftCardStatus.ACTIVE).index()
    val issuedBy = reference("issued_by", UserTable.id).nullable()
    val expiresAt = datetime("expires_at").nullable()
}

class GiftCardDAO(id: EntityID<String>) : BaseEntity(id, GiftCardTable) {
    companion object : BaseEntityClass<GiftCardDAO>(GiftCardTable, GiftCardDAO::class.java)

    var code by GiftCardTable.code
    var initialBalance by GiftCardTable.initialBalance
    var balance by GiftCardTable.balance
    var currency by GiftCardTable.currency
    var status by GiftCardTable.status
    var issuedBy by GiftCardTable.issuedBy
    var expiresAt by GiftCardTable.expiresAt

    fun isUsable(): Boolean =
        status == GiftCardStatus.ACTIVE &&
            balance > BigDecimal.ZERO &&
            (expiresAt?.isAfter(currentUtc()) ?: true)
}

object GiftCardRedemptionTable : BaseIdTable("gift_card_redemption") {
    val giftCardId = reference("gift_card_id", GiftCardTable.id).index()
    val orderId = reference("order_id", OrderTable.id).index()
    val userId = reference("user_id", UserTable.id)
    val amount = decimal("amount", 12, 2)
    val reversedAt = datetime("reversed_at").nullable()
}

class GiftCardRedemptionDAO(id: EntityID<String>) : BaseEntity(id, GiftCardRedemptionTable) {
    companion object : BaseEntityClass<GiftCardRedemptionDAO>(GiftCardRedemptionTable, GiftCardRedemptionDAO::class.java)

    var giftCardId by GiftCardRedemptionTable.giftCardId
    var orderId by GiftCardRedemptionTable.orderId
    var userId by GiftCardRedemptionTable.userId
    var amount by GiftCardRedemptionTable.amount
    var reversedAt by GiftCardRedemptionTable.reversedAt
}
