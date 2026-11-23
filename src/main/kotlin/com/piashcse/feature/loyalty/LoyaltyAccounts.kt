package com.piashcse.feature.loyalty

import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.WalletAccountDAO
import com.piashcse.database.entities.WalletAccountTable
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq

/** Locked wallet-account creation used by points, credit and referral flows. */
internal object LoyaltyAccounts {
    fun accountFor(userId: String): WalletAccountDAO {
        UserDAO.findById(userId) ?: userId.throwNotFound("User")
        // Lock the wallet row: point/credit redemption is a read-modify-write
        // operation and concurrent checkouts must not spend the same balance twice.
        return WalletAccountDAO.find { WalletAccountTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable) }
            .forUpdate()
            .firstOrNull()
            ?: WalletAccountDAO.new {
                this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
                referralCode = uniqueReferralCode()
            }
    }

    private fun uniqueReferralCode(): String {
        repeat(10) {
            val code = LoyaltyCodes.random(8)
            if (!WalletAccountDAO.find { WalletAccountTable.referralCode eq code }.any()) return code
        }
        throw ValidationException("Could not allocate a referral code")
    }
}
