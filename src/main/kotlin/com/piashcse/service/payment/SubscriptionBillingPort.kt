package com.piashcse.service.payment

import java.math.BigDecimal

/**
 * Payment-owned view of subscription billing. Implemented by the subscription
 * module so payment can apply provider webhooks without depending on it.
 */
interface SubscriptionBillingPort {
    fun applyPaidInvoiceTx(
        invoiceId: String,
        providerReference: String?,
        paidAmount: BigDecimal? = null,
        currency: String? = null,
    ): Boolean

    fun applyFailedInvoiceTx(
        invoiceId: String,
        reason: String,
    ): Boolean
}
