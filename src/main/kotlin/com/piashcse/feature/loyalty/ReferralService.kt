package com.piashcse.feature.loyalty

import com.piashcse.constants.AppConstants
import com.piashcse.constants.ReferralStatus
import com.piashcse.constants.WalletEntryType
import com.piashcse.database.entities.ReferralDAO
import com.piashcse.database.entities.ReferralTable
import com.piashcse.database.entities.WalletAccountDAO
import com.piashcse.database.entities.WalletAccountTable
import com.piashcse.database.entities.WalletLedgerDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import java.math.BigDecimal

/** Referral claim, listing and completion rewards. */
class ReferralService {
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
        val alreadyPurchased =
            com.piashcse.database.entities.OrderDAO.find {
                (com.piashcse.database.entities.OrderTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable)) and
                    (com.piashcse.database.entities.OrderTable.paymentStatus eq com.piashcse.constants.PaymentStatus.COMPLETED)
            }.any()
        if (alreadyPurchased) throw ValidationException("Referral codes are only for new customers")
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

    fun completeReferralTx(refereeUserId: String) {
        val referral =
            ReferralDAO.find {
                (ReferralTable.refereeUserId eq refereeUserId.entityID(com.piashcse.database.entities.UserTable)) and
                    (ReferralTable.status eq ReferralStatus.PENDING)
            }.firstOrNull() ?: return
        val referrer = LoyaltyAccounts.accountFor(referral.referrerUserId.value)
        val referee = LoyaltyAccounts.accountFor(refereeUserId)
        listOf(referrer, referee).forEach { account ->
            account.pointsBalance += referral.rewardPoints
            account.lifetimePoints += referral.rewardPoints
            WalletLedgerDAO.new {
                this.userId = account.userId
                this.type = WalletEntryType.REFERRAL_BONUS
                this.pointsDelta = referral.rewardPoints
                this.referenceType = WalletReferenceType.REFERRAL
                this.referenceId = referral.id.value
                this.note = "Referral bonus"
            }
        }
        referral.status = ReferralStatus.COMPLETED
        referral.completedAt = currentUtc()
    }

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
}
