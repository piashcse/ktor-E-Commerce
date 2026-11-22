package com.piashcse.feature.loyalty

import com.piashcse.database.entities.WalletAccountDAO
import com.piashcse.database.entities.WalletLedgerDAO
import com.piashcse.database.entities.WalletLedgerTable
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Wallet balance and ledger reads. */
internal object WalletQueries {
    suspend fun wallet(userId: String): WalletResponse =
        query {
            val account = LoyaltyAccounts.accountFor(userId)
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
}

internal fun WalletAccountDAO.toWalletResponse() =
    WalletResponse(
        creditBalance = Money.str(creditBalance),
        pointsBalance = pointsBalance,
        lifetimePoints = lifetimePoints,
        referralCode = referralCode,
    )

internal fun WalletLedgerDAO.toLedgerResponse() =
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
