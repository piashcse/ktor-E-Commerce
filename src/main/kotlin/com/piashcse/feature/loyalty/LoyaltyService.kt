package com.piashcse.feature.loyalty

import com.piashcse.constants.AppConstants
import com.piashcse.constants.GiftCardStatus
import com.piashcse.constants.ReferralStatus
import com.piashcse.constants.WalletEntryType
import com.piashcse.utils.common.PaginatedResponse
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

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
    val currency: String = AppConstants.DEFAULT_CURRENCY,
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
    private val orderEffects = OrderLoyaltyEffects()

    private val referrals = ReferralService()

    private val giftCards = GiftCardService()

    suspend fun wallet(userId: String): WalletResponse = WalletQueries.wallet(userId)

    suspend fun ledger(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<WalletLedgerResponse> = WalletQueries.ledger(userId, limit, offset)

    fun earnForPaidOrderTx(
        userId: String,
        orderId: String,
        orderTotal: BigDecimal,
    ) = orderEffects.earnForPaidOrderTx(userId, orderId, orderTotal)

    fun redemptionPreviewTx(
        userId: String,
        points: Int,
        walletCreditUsd: BigDecimal?,
        giftCardCode: String?,
    ): RedemptionPreview = WalletRedemption.redemptionPreviewTx(userId, points, walletCreditUsd, giftCardCode)

    fun reverseOrderTx(
        userId: String,
        orderId: String,
    ) = orderEffects.reverseOrderTx(userId, orderId)

    fun restorePartialInstrumentsTx(
        userId: String,
        orderId: String,
        walletUsd: BigDecimal,
        giftUsd: BigDecimal,
        refundId: String,
    ) = orderEffects.restorePartialInstrumentsTx(userId, orderId, walletUsd, giftUsd, refundId)

    suspend fun claimReferral(
        userId: String,
        code: String,
    ): ReferralResponse = referrals.claimReferral(userId, code)

    fun claimReferralTx(
        userId: String,
        code: String,
    ): ReferralResponse = referrals.claimReferralTx(userId, code)

    suspend fun listReferrals(userId: String): List<ReferralResponse> = referrals.listReferrals(userId)

    fun redeemPointsTx(
        userId: String,
        points: Int,
        referenceId: String,
    ): BigDecimal = WalletRedemption.redeemPointsTx(userId, points, referenceId)

    fun redeemCreditTx(
        userId: String,
        amountUsd: BigDecimal,
        referenceId: String,
    ): BigDecimal = WalletRedemption.redeemCreditTx(userId, amountUsd, referenceId)

    /** Delegates to [GiftCardService]; kept for checkout callers. */
    fun redeemGiftCardTx(
        code: String,
        userId: String,
        orderId: String,
        amountUsd: BigDecimal,
    ): BigDecimal = giftCards.redeemGiftCardTx(code, userId, orderId, amountUsd)

    suspend fun giftCard(code: String): GiftCardResponse = giftCards.giftCard(code)

    suspend fun issueGiftCard(
        issuedBy: String,
        request: GiftCardIssueRequest,
    ): GiftCardResponse = giftCards.issueGiftCard(issuedBy, request)

    suspend fun listGiftCards(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<GiftCardResponse> = giftCards.listGiftCards(limit, offset)

    suspend fun voidGiftCard(id: String): Boolean = giftCards.voidGiftCard(id)

    suspend fun voidGiftCard(
        actorUserId: String,
        id: String,
    ): Boolean = giftCards.voidGiftCard(actorUserId, id)

    suspend fun adminCredit(
        userId: String,
        request: AdminCreditRequest,
    ): WalletResponse = WalletRedemption.adminCredit(userId, request)

    suspend fun adminCredit(
        actorUserId: String,
        userId: String,
        request: AdminCreditRequest,
    ): WalletResponse = WalletRedemption.adminCredit(actorUserId, userId, request)
}
