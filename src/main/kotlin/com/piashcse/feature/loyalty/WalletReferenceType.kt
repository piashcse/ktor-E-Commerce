package com.piashcse.feature.loyalty

/**
 * Ledger reference types written by [LoyaltyService].
 *
 * These values are persisted in `wallet_ledger.reference_type`, so they are
 * part of the data contract and must stay stable.
 */
object WalletReferenceType {
    const val ORDER = "ORDER"
    const val ORDER_PARTIAL = "ORDER_PARTIAL"
    const val REFERRAL = "REFERRAL"
}
