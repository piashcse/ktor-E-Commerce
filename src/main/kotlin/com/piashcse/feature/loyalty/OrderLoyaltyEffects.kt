package com.piashcse.feature.loyalty

import com.piashcse.constants.AppConstants
import com.piashcse.constants.GiftCardStatus
import com.piashcse.constants.WalletEntryType
import com.piashcse.database.entities.GiftCardDAO
import com.piashcse.database.entities.GiftCardRedemptionDAO
import com.piashcse.database.entities.GiftCardRedemptionTable
import com.piashcse.database.entities.GiftCardTable
import com.piashcse.database.entities.WalletLedgerDAO
import com.piashcse.database.entities.WalletLedgerTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.extension.entityID
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import java.math.BigDecimal
import java.math.RoundingMode

/** Points earning, order reversal and partial instrument restoration. */
internal class OrderLoyaltyEffects(
    private val referrals: ReferralService = ReferralService(),
) {
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
                    (WalletLedgerTable.referenceType eq WalletReferenceType.ORDER) and
                    (WalletLedgerTable.referenceId eq orderId)
            }.any()
        if (!alreadyAwarded) {
            val points =
                orderTotal
                    .setScale(0, RoundingMode.DOWN)
                    .toInt()
                    .times(AppConstants.LOYALTY_POINTS_PER_USD)
            if (points > 0) {
                val account = LoyaltyAccounts.accountFor(userId)
                account.pointsBalance += points
                account.lifetimePoints += points
                WalletLedgerDAO.new {
                    this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
                    this.type = WalletEntryType.POINTS_EARN
                    this.pointsDelta = points
                    this.referenceType = WalletReferenceType.ORDER
                    this.referenceId = orderId
                    this.note = "Points earned on order $orderId"
                }
            }
        }
        referrals.completeReferralTx(userId)
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
                    (WalletLedgerTable.referenceType eq WalletReferenceType.ORDER) and
                    (WalletLedgerTable.referenceId eq orderId) and
                    (WalletLedgerTable.type inList listOf(WalletEntryType.POINTS_REVERSAL, WalletEntryType.CREDIT_REVERSAL))
            }.any()
        if (alreadyReversed) return

        val account = LoyaltyAccounts.accountFor(userId)
        val entries =
            WalletLedgerDAO.find {
                (WalletLedgerTable.userId eq user) and
                    (WalletLedgerTable.referenceType eq WalletReferenceType.ORDER) and
                    (WalletLedgerTable.referenceId eq orderId)
            }.toList()
        var pointsToRestore = 0
        var creditToRestore = BigDecimal.ZERO
        entries.forEach { entry ->
            when (entry.type) {
                WalletEntryType.POINTS_EARN ->
                    // Allow a negative balance: if the user already spent the
                    // points, the debt must be recovered from future earnings
                    // instead of being erased on refund.
                    account.pointsBalance -= entry.pointsDelta
                WalletEntryType.POINTS_REDEEM -> pointsToRestore += -entry.pointsDelta
                WalletEntryType.CREDIT_REDEEM -> creditToRestore = creditToRestore.add(entry.creditDelta.negate())
                else -> Unit
            }
        }
        val alreadyRestored =
            WalletLedgerDAO.find {
                (WalletLedgerTable.userId eq user) and
                    (WalletLedgerTable.referenceType eq WalletReferenceType.ORDER_PARTIAL) and
                    (WalletLedgerTable.referenceId eq orderId)
            }.toList()
        pointsToRestore =
            (
                pointsToRestore -
                    alreadyRestored.filter { it.type == WalletEntryType.POINTS_REVERSAL }.sumOf { it.pointsDelta }
            )
                .coerceAtLeast(0)
        creditToRestore =
            creditToRestore
                .subtract(
                    alreadyRestored.filter { it.type == WalletEntryType.CREDIT_REVERSAL }
                        .fold(BigDecimal.ZERO) { acc, entry -> acc.add(entry.creditDelta) },
                )
                .max(BigDecimal.ZERO)

        if (pointsToRestore > 0) {
            account.pointsBalance += pointsToRestore
            WalletLedgerDAO.new {
                this.userId = user
                this.type = WalletEntryType.POINTS_REVERSAL
                this.pointsDelta = pointsToRestore
                this.referenceType = WalletReferenceType.ORDER
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
                this.referenceType = WalletReferenceType.ORDER
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
                this.referenceType = WalletReferenceType.ORDER
                this.referenceId = orderId
                this.note = "Order reversal marker"
            }
        }

        GiftCardRedemptionDAO.find { GiftCardRedemptionTable.orderId eq orderId.entityID(com.piashcse.database.entities.OrderTable) }
            .forUpdate()
            .forEach { redemption ->
                if (redemption.reversedAt == null) {
                    val card =
                        GiftCardDAO.find { GiftCardTable.id eq redemption.giftCardId }
                            .forUpdate()
                            .firstOrNull() ?: return@forEach
                    card.balance = card.balance.add(redemption.amount).setScale(2, RoundingMode.HALF_UP)
                    if (card.status == GiftCardStatus.REDEEMED && card.expiresAt?.isBefore(currentUtc()) != true) {
                        card.status = GiftCardStatus.ACTIVE
                    }
                    redemption.reversedAt = currentUtc()
                }
            }
    }

    /**
     * Restore the wallet/gift share of one partially refunded item. Uses
     * ORDER_PARTIAL ledger rows so a later full refund restores only the
     * remaining instrument value.
     */
    fun restorePartialInstrumentsTx(
        userId: String,
        orderId: String,
        walletUsd: BigDecimal,
        giftUsd: BigDecimal,
        refundId: String,
    ) {
        if (walletUsd <= BigDecimal.ZERO && giftUsd <= BigDecimal.ZERO) return
        val user = userId.entityID(com.piashcse.database.entities.UserTable)
        val account = LoyaltyAccounts.accountFor(userId)

        if (walletUsd > BigDecimal.ZERO) {
            val orderEntries =
                WalletLedgerDAO.find {
                    (WalletLedgerTable.userId eq user) and
                        (WalletLedgerTable.referenceType eq WalletReferenceType.ORDER) and
                        (WalletLedgerTable.referenceId eq orderId)
                }.toList()
            val pointsUsd =
                BigDecimal(orderEntries.filter { it.type == WalletEntryType.POINTS_REDEEM }.sumOf { -it.pointsDelta })
                    .divide(BigDecimal(AppConstants.LOYALTY_POINTS_PER_USD_REDEEM), 2, RoundingMode.HALF_UP)
            val creditUsd =
                orderEntries.filter { it.type == WalletEntryType.CREDIT_REDEEM }
                    .fold(BigDecimal.ZERO) { acc, entry -> acc.add(entry.creditDelta.negate()) }
            val totalUsd = pointsUsd.add(creditUsd)
            if (totalUsd > BigDecimal.ZERO) {
                val alreadyRestored =
                    WalletLedgerDAO.find {
                        (WalletLedgerTable.userId eq user) and
                            (WalletLedgerTable.referenceType eq WalletReferenceType.ORDER_PARTIAL) and
                            (WalletLedgerTable.referenceId eq orderId)
                    }.toList()
                val pointsAlready =
                    alreadyRestored.filter { it.type == WalletEntryType.POINTS_REVERSAL }.sumOf { it.pointsDelta }
                val creditAlready =
                    alreadyRestored.filter { it.type == WalletEntryType.CREDIT_REVERSAL }
                        .fold(BigDecimal.ZERO) { acc, entry -> acc.add(entry.creditDelta) }
                val remainingUsd =
                    walletUsd.min(
                        totalUsd.subtract(pointsAlreadyUsd(pointsAlready)).subtract(creditAlready).max(BigDecimal.ZERO),
                    )
                val pointsShare =
                    remainingUsd.multiply(pointsUsd).divide(totalUsd, 2, RoundingMode.HALF_UP)
                val points =
                    pointsShare.multiply(BigDecimal(AppConstants.LOYALTY_POINTS_PER_USD_REDEEM))
                        .setScale(0, RoundingMode.DOWN).toInt()
                val credit = remainingUsd.subtract(pointsShare).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
                if (points > 0) {
                    account.pointsBalance += points
                    WalletLedgerDAO.new {
                        this.userId = user
                        type = WalletEntryType.POINTS_REVERSAL
                        pointsDelta = points
                        referenceType = WalletReferenceType.ORDER_PARTIAL
                        this.referenceId = orderId
                        note = "Partial refund $refundId"
                    }
                }
                if (credit > BigDecimal.ZERO) {
                    account.creditBalance = account.creditBalance.add(credit).setScale(2, RoundingMode.HALF_UP)
                    WalletLedgerDAO.new {
                        this.userId = user
                        type = WalletEntryType.CREDIT_REVERSAL
                        creditDelta = credit
                        referenceType = WalletReferenceType.ORDER_PARTIAL
                        this.referenceId = orderId
                        note = "Partial refund $refundId"
                    }
                }
            }
        }

        if (giftUsd > BigDecimal.ZERO) {
            // Negative redemption rows record the partial restoration and are
            // netted by the final reverseOrderTx.
            val remainingByCard =
                GiftCardRedemptionDAO.find {
                    GiftCardRedemptionTable.orderId eq orderId.entityID(com.piashcse.database.entities.OrderTable)
                }.toList()
                    .groupBy { it.giftCardId.value }
                    .mapValues { (_, rows) -> rows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.amount) } }
                    .filterValues { it > BigDecimal.ZERO }
                    .toMutableMap()
            var remaining = giftUsd
            remainingByCard.keys.forEach { cardId ->
                if (remaining <= BigDecimal.ZERO) return@forEach
                val take = remaining.min(remainingByCard[cardId] ?: BigDecimal.ZERO)
                if (take <= BigDecimal.ZERO) return@forEach
                val card =
                    GiftCardDAO.find { GiftCardTable.id eq cardId.entityID(GiftCardTable) }
                        .forUpdate()
                        .firstOrNull() ?: return@forEach
                card.balance = card.balance.add(take).setScale(2, RoundingMode.HALF_UP)
                if (card.status == GiftCardStatus.REDEEMED && card.expiresAt?.isBefore(currentUtc()) != true) {
                    card.status = GiftCardStatus.ACTIVE
                }
                GiftCardRedemptionDAO.new {
                    this.giftCardId = card.id
                    this.orderId = orderId.entityID(com.piashcse.database.entities.OrderTable)
                    this.userId = user
                    amount = take.negate()
                }
                remaining = remaining.subtract(take)
            }
        }
    }

    private fun pointsAlreadyUsd(points: Int): BigDecimal =
        BigDecimal(points).divide(BigDecimal(AppConstants.LOYALTY_POINTS_PER_USD_REDEEM), 2, RoundingMode.HALF_UP)
}
