package com.piashcse.feature.loyalty

import com.piashcse.constants.AppConstants
import com.piashcse.constants.WalletEntryType
import com.piashcse.database.entities.GiftCardDAO
import com.piashcse.database.entities.GiftCardTable
import com.piashcse.database.entities.WalletLedgerDAO
import com.piashcse.database.entities.WalletLedgerTable
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.math.BigDecimal
import java.math.RoundingMode

/** Points, store-credit and admin-adjustment operations. */
internal object WalletRedemption {
    fun redemptionPreviewTx(
        userId: String,
        points: Int,
        walletCreditUsd: BigDecimal?,
        giftCardCode: String?,
    ): RedemptionPreview {
        val account = LoyaltyAccounts.accountFor(userId)
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
     * Redeems points into USD. Must run inside the checkout transaction.
     * Returns the USD value; returns zero when the same order already redeemed.
     */
    fun redeemPointsTx(
        userId: String,
        points: Int,
        referenceId: String,
    ): BigDecimal {
        if (points <= 0) return BigDecimal.ZERO
        val account = LoyaltyAccounts.accountFor(userId)
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
            this.referenceType = WalletReferenceType.ORDER
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
        val account = LoyaltyAccounts.accountFor(userId)
        if (account.creditBalance < amountUsd) {
            throw ValidationException("Insufficient store credit: ${account.creditBalance.toPlainString()} available")
        }
        if (alreadyRedeemed(userId, WalletEntryType.CREDIT_REDEEM, referenceId)) return BigDecimal.ZERO
        account.creditBalance = account.creditBalance.subtract(amountUsd).setScale(2, RoundingMode.HALF_UP)
        WalletLedgerDAO.new {
            this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
            this.type = WalletEntryType.CREDIT_REDEEM
            this.creditDelta = amountUsd.negate()
            this.referenceType = WalletReferenceType.ORDER
            this.referenceId = referenceId
            this.note = "Store credit used at checkout"
        }
        return amountUsd
    }

    /** Legacy/test call without an actor; audited as SYSTEM. */
    suspend fun adminCredit(
        userId: String,
        request: AdminCreditRequest,
    ): WalletResponse = adminCredit("system", userId, request)

    suspend fun adminCredit(
        actorUserId: String,
        userId: String,
        request: AdminCreditRequest,
    ): WalletResponse =
        query {
            if (request.amount <= BigDecimal.ZERO) throw ValidationException("Credit amount must be positive")
            val account = LoyaltyAccounts.accountFor(userId)
            account.creditBalance = account.creditBalance.add(request.amount).setScale(2, RoundingMode.HALF_UP)
            WalletLedgerDAO.new {
                this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
                this.type = WalletEntryType.ADMIN_ADJUSTMENT
                this.creditDelta = request.amount
                this.note = request.note ?: "Admin store-credit adjustment"
            }
            com.piashcse.event.AdminAudit.publish(
                actorUserId,
                "WALLET_CREDITED",
                "WALLET",
                userId,
                "Credit ${request.amount.toPlainString()}",
            )
            account.toWalletResponse()
        }

    private fun alreadyRedeemed(
        userId: String,
        type: WalletEntryType,
        referenceId: String,
    ): Boolean =
        WalletLedgerDAO.find {
            (WalletLedgerTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable)) and
                (WalletLedgerTable.type eq type) and
                (WalletLedgerTable.referenceType eq WalletReferenceType.ORDER) and
                (WalletLedgerTable.referenceId eq referenceId)
        }.any()
}
