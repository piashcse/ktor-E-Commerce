package com.piashcse.event

/**
 * Stable aggregate names used by the transactional outbox.
 *
 * Keeping these in one place prevents typos in `aggregate_type` values, which
 * would otherwise silently split an aggregate's event stream.
 */
object OutboxAggregate {
    const val AUDIT = "AUDIT"
    const val ORDER = "ORDER"
    const val PAYMENT = "PAYMENT"
    const val PRODUCT = "PRODUCT"
    const val REFUND = "REFUND"
    const val RMA = "RMA"
    const val SHOP = "SHOP"
    const val SUPPORT = "SUPPORT"
    const val B2B_INVOICE = "B2B_INVOICE"
}
