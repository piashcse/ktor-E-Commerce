package com.piashcse.feature.loyalty

import com.piashcse.constants.AppConstants
import com.piashcse.constants.GiftCardStatus
import com.piashcse.constants.ReferralStatus
import com.piashcse.constants.WalletEntryType
import com.piashcse.database.entities.GiftCardDAO
import com.piashcse.database.entities.GiftCardRedemptionDAO
import com.piashcse.database.entities.GiftCardRedemptionTable
import com.piashcse.database.entities.GiftCardTable
import com.piashcse.database.entities.ReferralDAO
import com.piashcse.database.entities.ReferralTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.WalletAccountDAO
import com.piashcse.database.entities.WalletAccountTable
import com.piashcse.database.entities.WalletLedgerDAO
import com.piashcse.database.entities.WalletLedgerTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.DateTimes
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
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.SecureRandom

@Serializable
data class WalletResponse(
    val creditBalance: String,
    val pointsBalance: Int,
    val lifetimePoints: Int,
    val referralCode: String,
)

@Serializable
data class WalletLedgerResponse(
    val id: String,
    val type: WalletEntryType,
    val pointsDelta: Int,
    val creditDelta: String,
    val referenceType: String?,
    val referenceId: String?,
    val note: String?,
    val createdAt: String?,
)

@Serializable
data class ReferralClaimRequest(val code: String)

@Serializable
data class ReferralResponse(
    val id: String,
    val referrerUserId: String,
    val refereeUserId: String,
    val code: String,
    val status: ReferralStatus,
    val rewardPoints: Int,
    val rewardCredit: String,
    val completedAt: String?,
    val createdAt: String?,
)

@Serializable
data class GiftCardIssueRequest(
    @Contextual val amount: BigDecimal,
    val currency: String = "USD",
    val code: String? = null,
    val expiresAt: String? = null,
)

@Serializable
data class GiftCardResponse(
    val id: String,
    val code: String,
    val initialBalance: String,
    val balance: String,
    val currency: String,
    val status: GiftCardStatus,
    val expiresAt: String?,
    val createdAt: String?,
)

@Serializable
data class AdminCreditRequest(
    @Contextual val amount: BigDecimal,
    val note: String? = null,
)

/**
 * Unified loyalty wallet:
 * - points earned on paid orders and redeemed at checkout
 * - store credit (USD) from refunds/goodwill/referrals
 * - gift cards redeemed against an order
 * - one-time referral rewards when the referee pays their first order
 */
data class RedemptionPreview(
    val pointsUsd: BigDecimal,
    val creditUsd: BigDecimal,
    val giftCardUsd: BigDecimal,
    val pointsAvailable: Int,
    val creditAvailable: BigDecimal,
)

class LoyaltyService {
    private val random = SecureRandom()

    suspend fun wallet(userId: String): WalletResponse =
        query {
            val account = accountFor(userId)
            account.toWalletResponse()
        }

    suspend fun ledger(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<WalletLedgerResponse> =
        query {
            WalletLedgerTable.selectAll()
                .where { WalletLedgerTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable) }
                .orderBy(WalletLedgerTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { WalletLedgerDAO.wrapRow(it).toLedgerResponse() }
        }

    /** Idempotently awards points for a paid order and completes a pending referral. */
    fun earnForPaidOrderTx(
        userId: String,
        orderId: String,
        orderTotal: BigDecimal,
    ) {
        val alreadyAwarded =
            WalletLedgerDAO.find {
                (WalletLedgerTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable)) and
                    (WalletLedgerTable.type eq WalletEntryType.POINTS_EARN) and
                    (WalletLedgerTable.referenceType eq "ORDER") and
                    (WalletLedgerTable.referenceId eq orderId)
            }.any()
        if (!alreadyAwarded) {
            val points =
                orderTotal
                    .setScale(0, RoundingMode.DOWN)
                    .toInt()
                    .times(AppConstants.LOYALTY_POINTS_PER_USD)
            if (points > 0) {
                val account = accountFor(userId)
                account.pointsBalance += points
                account.lifetimePoints += points
                WalletLedgerDAO.new {
                    this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
                    this.type = WalletEntryType.POINTS_EARN
                    this.pointsDelta = points
                    this.referenceType = "ORDER"
                    this.referenceId = orderId
                    this.note = "Points earned on order $orderId"
                }
            }
        }
        completeReferralTx(userId)
    }

    /** Read-only preview used by checkout summary; never mutates balances. */
    fun redemptionPreviewTx(
        userId: String,
        points: Int,
        walletCreditUsd: BigDecimal?,
        giftCardCode: String?,
    ): RedemptionPreview {
        val account = accountFor(userId)
        val pointsToUse = points.coerceIn(0, account.pointsBalance)
        val pointsUsd =
            BigDecimal(pointsToUse)
                .divide(BigDecimal(AppConstants.LOYALTY_POINTS_PER_USD_REDEEM), 2, RoundingMode.HALF_UP)
        val creditUsd = walletCreditUsd?.min(account.creditBalance)?.max(BigDecimal.ZERO) ?: BigDecimal.ZERO
        val giftUsd =
            giftCardCode?.takeIf { it.isNotBlank() }?.let { code ->
                GiftCardDAO.find { GiftCardTable.code eq code.trim().uppercase() }
                    .firstOrNull()
                    ?.takeIf { it.isUsable() }
                    ?.balance ?: BigDecimal.ZERO
            } ?: BigDecimal.ZERO
        return RedemptionPreview(
            pointsUsd = pointsUsd.setScale(2, RoundingMode.HALF_UP),
            creditUsd = creditUsd.setScale(2, RoundingMode.HALF_UP),
            giftCardUsd = giftUsd.setScale(2, RoundingMode.HALF_UP),
            pointsAvailable = account.pointsBalance,
            creditAvailable = account.creditBalance,
        )
    }

    /**
     * Reverses points/credit earned or spent on an order and restores gift-card redemptions.
     * Idempotent per order. Must run inside the cancellation/refund transaction.
     */
    fun reverseOrderTx(
        userId: String,
        orderId: String,
    ) {
        val user = userId.entityID(com.piashcse.database.entities.UserTable)
        val alreadyReversed =
            WalletLedgerDAO.find {
                (WalletLedgerTable.userId eq user) and
                    (WalletLedgerTable.referenceType eq "ORDER") and
                    (WalletLedgerTable.referenceId eq orderId) and
                    (WalletLedgerTable.type inList listOf(WalletEntryType.POINTS_REVERSAL, WalletEntryType.CREDIT_REVERSAL))
            }.any()
        if (alreadyReversed) return

        val account = accountFor(userId)
        val entries =
            WalletLedgerDAO.find {
                (WalletLedgerTable.userId eq user) and
                    (WalletLedgerTable.referenceType eq "ORDER") and
                    (WalletLedgerTable.referenceId eq orderId)
            }.toList()
        var pointsToRestore = 0
        var creditToRestore = BigDecimal.ZERO
        entries.forEach { entry ->
            when (entry.type) {
                WalletEntryType.POINTS_EARN ->
                    account.pointsBalance = (account.pointsBalance - entry.pointsDelta).coerceAtLeast(0)
                WalletEntryType.POINTS_REDEEM -> pointsToRestore += -entry.pointsDelta
                WalletEntryType.CREDIT_REDEEM -> creditToRestore = creditToRestore.add(entry.creditDelta.negate())
                else -> Unit
            }
        }
        if (pointsToRestore > 0) {
            account.pointsBalance += pointsToRestore
            WalletLedgerDAO.new {
                this.userId = user
                this.type = WalletEntryType.POINTS_REVERSAL
                this.pointsDelta = pointsToRestore
                this.referenceType = "ORDER"
                this.referenceId = orderId
                this.note = "Points restored after order reversal"
            }
        }
        if (creditToRestore > BigDecimal.ZERO) {
            account.creditBalance = account.creditBalance.add(creditToRestore).setScale(2, RoundingMode.HALF_UP)
            WalletLedgerDAO.new {
                this.userId = user
                this.type = WalletEntryType.CREDIT_REVERSAL
                this.creditDelta = creditToRestore
                this.referenceType = "ORDER"
                this.referenceId = orderId
                this.note = "Store credit restored after order reversal"
            }
        }
        if (entries.isNotEmpty() && pointsToRestore == 0 && creditToRestore <= BigDecimal.ZERO) {
            // Marker so a repeated reversal cannot subtract earned points twice.
            WalletLedgerDAO.new {
                this.userId = user
                this.type = WalletEntryType.POINTS_REVERSAL
                this.pointsDelta = 0
                this.referenceType = "ORDER"
                this.referenceId = orderId
                this.note = "Order reversal marker"
            }
        }

        GiftCardRedemptionDAO.find { GiftCardRedemptionTable.orderId eq orderId.entityID(com.piashcse.database.entities.OrderTable) }
            .forEach { redemption ->
                if (redemption.reversedAt == null) {
                    val card = GiftCardDAO.findById(redemption.giftCardId.value) ?: return@forEach
                    card.balance = card.balance.add(redemption.amount).setScale(2, RoundingMode.HALF_UP)
                    if (card.status == GiftCardStatus.REDEEMED && card.expiresAt?.isBefore(currentUtc()) != true) {
                        card.status = GiftCardStatus.ACTIVE
                    }
                    redemption.reversedAt = currentUtc()
                }
            }
    }

    suspend fun claimReferral(
        userId: String,
        code: String,
    ): ReferralResponse = query { claimReferralTx(userId, code) }

    fun claimReferralTx(
        userId: String,
        code: String,
    ): ReferralResponse {
        if (code.isBlank()) throw ValidationException("Referral code is required")
        val normalized = code.trim().uppercase()
        val referrer =
            WalletAccountDAO.find { WalletAccountTable.referralCode eq normalized }.firstOrNull()
                ?: throw ValidationException("Referral code not found")
        if (referrer.userId.value == userId) throw ValidationException("You cannot refer yourself")
        if (ReferralDAO.find { ReferralTable.refereeUserId eq userId.entityID(com.piashcse.database.entities.UserTable) }.any()) {
            throw ValidationException("A referral has already been claimed")
        }
        return ReferralDAO.new {
            this.referrerUserId = referrer.userId
            this.refereeUserId = userId.entityID(com.piashcse.database.entities.UserTable)
            this.code = normalized
            this.status = ReferralStatus.PENDING
            this.rewardPoints = AppConstants.REFERRAL_REWARD_POINTS
            this.rewardCredit = BigDecimal.ZERO
        }.toReferralResponse()
    }

    suspend fun listReferrals(userId: String): List<ReferralResponse> =
        query {
            ReferralDAO.find {
                (ReferralTable.referrerUserId eq userId.entityID(com.piashcse.database.entities.UserTable)) or
                    (ReferralTable.refereeUserId eq userId.entityID(com.piashcse.database.entities.UserTable))
            }.map { it.toReferralResponse() }
        }

    /**
     * Redeems points into USD. Must run inside the checkout transaction.
     * Returns the USD value; returns zero when the same order already redeemed.
     */
    fun redeemPointsTx(
        userId: String,
        points: Int,
        referenceId: String,
    ): BigDecimal {
        if (points <= 0) return BigDecimal.ZERO
        val account = accountFor(userId)
        if (account.pointsBalance < points) {
            throw ValidationException("Insufficient points: ${account.pointsBalance} available")
        }
        if (alreadyRedeemed(userId, WalletEntryType.POINTS_REDEEM, referenceId)) return BigDecimal.ZERO
        val value =
            BigDecimal(points)
                .divide(BigDecimal(AppConstants.LOYALTY_POINTS_PER_USD_REDEEM), 2, RoundingMode.HALF_UP)
        account.pointsBalance -= points
        WalletLedgerDAO.new {
            this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
            this.type = WalletEntryType.POINTS_REDEEM
            this.pointsDelta = -points
            this.referenceType = "ORDER"
            this.referenceId = referenceId
            this.note = "Redeemed $points points"
        }
        return value
    }

    /** Debits store credit (USD). Returns the requested amount or zero for a replay. */
    fun redeemCreditTx(
        userId: String,
        amountUsd: BigDecimal,
        referenceId: String,
    ): BigDecimal {
        if (amountUsd <= BigDecimal.ZERO) return BigDecimal.ZERO
        val account = accountFor(userId)
        if (account.creditBalance < amountUsd) {
            throw ValidationException("Insufficient store credit: ${account.creditBalance.toPlainString()} available")
        }
        if (alreadyRedeemed(userId, WalletEntryType.CREDIT_REDEEM, referenceId)) return BigDecimal.ZERO
        account.creditBalance = account.creditBalance.subtract(amountUsd).setScale(2, RoundingMode.HALF_UP)
        WalletLedgerDAO.new {
            this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
            this.type = WalletEntryType.CREDIT_REDEEM
            this.creditDelta = amountUsd.negate()
            this.referenceType = "ORDER"
            this.referenceId = referenceId
            this.note = "Store credit used at checkout"
        }
        return amountUsd
    }

    /** Debits a gift card (USD). Returns the redeemed USD amount, capped by the card balance. */
    fun redeemGiftCardTx(
        code: String,
        userId: String,
        orderId: String,
        amountUsd: BigDecimal,
    ): BigDecimal {
        if (code.isBlank() || amountUsd <= BigDecimal.ZERO) return BigDecimal.ZERO
        val card =
            GiftCardDAO.find { GiftCardTable.code eq code.trim().uppercase() }.forUpdate().firstOrNull()
                ?: throw ValidationException("Gift card not found")
        if (!card.isUsable()) throw ValidationException("Gift card is not active or has expired")
        if (card.currency != "USD") throw ValidationException("Only USD gift cards are supported")
        val amount = amountUsd.min(card.balance).setScale(2, RoundingMode.HALF_UP)
        card.balance = card.balance.subtract(amount).setScale(2, RoundingMode.HALF_UP)
        if (card.balance <= BigDecimal.ZERO) card.status = GiftCardStatus.REDEEMED
        GiftCardRedemptionDAO.new {
            this.giftCardId = card.id
            this.orderId = orderId.entityID(com.piashcse.database.entities.OrderTable)
            this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
            this.amount = amount
        }
        return amount
    }

    suspend fun giftCard(code: String): GiftCardResponse =
        query {
            val card =
                GiftCardDAO.find { GiftCardTable.code eq code.trim().uppercase() }.firstOrNull()
                    ?: throw ValidationException("Gift card not found")
            card.toGiftCardResponse()
        }

    suspend fun issueGiftCard(
        issuedBy: String,
        request: GiftCardIssueRequest,
    ): GiftCardResponse =
        query {
            if (request.amount <= BigDecimal.ZERO) {
                throw FieldValidationException(listOf(FieldViolation("amount", "Amount must be positive")))
            }
            val code = request.code?.trim()?.uppercase()?.takeIf { it.isNotBlank() } ?: uniqueGiftCardCode()
            if (GiftCardDAO.find { GiftCardTable.code eq code }.any()) {
                throw ValidationException("Gift card code already exists")
            }
            GiftCardDAO.new {
                this.code = code
                this.initialBalance = request.amount.setScale(2, RoundingMode.HALF_UP)
                this.balance = request.amount.setScale(2, RoundingMode.HALF_UP)
                this.currency = request.currency.uppercase()
                this.status = GiftCardStatus.ACTIVE
                this.issuedBy = issuedBy.entityID(com.piashcse.database.entities.UserTable)
                this.expiresAt = DateTimes.parseFlexibleOrNull(request.expiresAt, "expiresAt")
            }.toGiftCardResponse()
        }

    suspend fun listGiftCards(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<GiftCardResponse> =
        query {
            GiftCardTable.selectAll()
                .orderBy(GiftCardTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { GiftCardDAO.wrapRow(it).toGiftCardResponse() }
        }

    suspend fun voidGiftCard(id: String): Boolean =
        query {
            val card = GiftCardDAO.findById(id) ?: return@query false
            card.status = GiftCardStatus.VOID
            true
        }

    suspend fun adminCredit(
        userId: String,
        request: AdminCreditRequest,
    ): WalletResponse =
        query {
            if (request.amount <= BigDecimal.ZERO) throw ValidationException("Credit amount must be positive")
            val account = accountFor(userId)
            account.creditBalance = account.creditBalance.add(request.amount).setScale(2, RoundingMode.HALF_UP)
            WalletLedgerDAO.new {
                this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
                this.type = WalletEntryType.ADMIN_ADJUSTMENT
                this.creditDelta = request.amount
                this.note = request.note ?: "Admin store-credit adjustment"
            }
            account.toWalletResponse()
        }

    private fun completeReferralTx(refereeUserId: String) {
        val referral =
            ReferralDAO.find {
                (ReferralTable.refereeUserId eq refereeUserId.entityID(com.piashcse.database.entities.UserTable)) and
                    (ReferralTable.status eq ReferralStatus.PENDING)
            }.firstOrNull() ?: return
        val referrer = accountFor(referral.referrerUserId.value)
        val referee = accountFor(refereeUserId)
        listOf(referrer, referee).forEach { account ->
            account.pointsBalance += referral.rewardPoints
            account.lifetimePoints += referral.rewardPoints
            WalletLedgerDAO.new {
                this.userId = account.userId
                this.type = WalletEntryType.REFERRAL_BONUS
                this.pointsDelta = referral.rewardPoints
                this.referenceType = "REFERRAL"
                this.referenceId = referral.id.value
                this.note = "Referral bonus"
            }
        }
        referral.status = ReferralStatus.COMPLETED
        referral.completedAt = currentUtc()
    }

    private fun alreadyRedeemed(
        userId: String,
        type: WalletEntryType,
        referenceId: String,
    ): Boolean =
        WalletLedgerDAO.find {
            (WalletLedgerTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable)) and
                (WalletLedgerTable.type eq type) and
                (WalletLedgerTable.referenceType eq "ORDER") and
                (WalletLedgerTable.referenceId eq referenceId)
        }.any()

    private fun accountFor(userId: String): WalletAccountDAO {
        UserDAO.findById(userId) ?: userId.throwNotFound("User")
        return WalletAccountDAO.find { WalletAccountTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable) }
            .firstOrNull()
            ?: WalletAccountDAO.new {
                this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
                referralCode = uniqueReferralCode()
            }
    }

    private fun uniqueReferralCode(): String {
        repeat(10) {
            val code = randomCode(8)
            if (!WalletAccountDAO.find { WalletAccountTable.referralCode eq code }.any()) return code
        }
        throw ValidationException("Could not allocate a referral code")
    }

    private fun uniqueGiftCardCode(): String {
        repeat(10) {
            val code = randomCode(16)
            if (!GiftCardDAO.find { GiftCardTable.code eq code }.any()) return code
        }
        throw ValidationException("Could not allocate a gift card code")
    }

    private fun randomCode(length: Int): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..length).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
    }

    private fun WalletAccountDAO.toWalletResponse() =
        WalletResponse(
            creditBalance = Money.str(creditBalance),
            pointsBalance = pointsBalance,
            lifetimePoints = lifetimePoints,
            referralCode = referralCode,
        )

    private fun WalletLedgerDAO.toLedgerResponse() =
        WalletLedgerResponse(
            id = id.value,
            type = type,
            pointsDelta = pointsDelta,
            creditDelta = Money.str(creditDelta),
            referenceType = referenceType,
            referenceId = referenceId,
            note = note,
            createdAt = createdAt.toUtcString(),
        )

    private fun ReferralDAO.toReferralResponse() =
        ReferralResponse(
            id = id.value,
            referrerUserId = referrerUserId.value,
            refereeUserId = refereeUserId.value,
            code = code,
            status = status,
            rewardPoints = rewardPoints,
            rewardCredit = Money.str(rewardCredit),
            completedAt = completedAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
        )

    private fun GiftCardDAO.toGiftCardResponse() =
        GiftCardResponse(
            id = id.value,
            code = code,
            initialBalance = Money.str(initialBalance),
            balance = Money.str(balance),
            currency = currency,
            status = status,
            expiresAt = expiresAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
        )
}
