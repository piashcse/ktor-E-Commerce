package com.piashcse.feature.loyalty

import com.piashcse.constants.AppConstants
import com.piashcse.constants.GiftCardStatus
import com.piashcse.database.entities.GiftCardDAO
import com.piashcse.database.entities.GiftCardRedemptionDAO
import com.piashcse.database.entities.GiftCardTable
import com.piashcse.utils.common.DateTimes
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.math.RoundingMode

/** Gift card issue, lookup, redemption and voiding. */
class GiftCardService {
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
        if (card.currency != AppConstants.DEFAULT_CURRENCY) throw ValidationException("Only USD gift cards are supported")
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
            if (!request.currency.equals(AppConstants.DEFAULT_CURRENCY, ignoreCase = true)) {
                throw ValidationException("Only USD gift cards are supported")
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
            }.also {
                com.piashcse.event.AdminAudit.publish(
                    issuedBy,
                    "GIFT_CARD_ISSUED",
                    "GIFT_CARD",
                    it.id.value,
                    "${it.initialBalance.toPlainString()} ${it.currency}",
                )
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

    suspend fun voidGiftCard(id: String): Boolean = voidGiftCard("system", id)

    suspend fun voidGiftCard(
        actorUserId: String,
        id: String,
    ): Boolean =
        query {
            val card = GiftCardDAO.findById(id) ?: return@query false
            card.status = GiftCardStatus.VOID
            com.piashcse.event.AdminAudit.publish(
                actorUserId,
                "GIFT_CARD_VOIDED",
                "GIFT_CARD",
                id,
                "Card voided",
            )
            true
        }

    private fun uniqueGiftCardCode(): String {
        repeat(10) {
            val code = LoyaltyCodes.random(16)
            if (!GiftCardDAO.find { GiftCardTable.code eq code }.any()) return code
        }
        throw ValidationException("Could not allocate a gift card code")
    }

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
