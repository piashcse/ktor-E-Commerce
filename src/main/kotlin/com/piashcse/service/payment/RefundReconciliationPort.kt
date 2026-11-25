package com.piashcse.service.payment

import java.math.BigDecimal

/**
 * Payment-owned view of refund reconciliation. Implemented by the refund module
 * so provider webhooks can update refund state without a feature-to-feature
 * dependency.
 */
interface RefundReconciliationPort {
    suspend fun reconcileProviderRefund(
        providerRefundId: String,
        status: String,
        amount: BigDecimal?,
    ): Boolean
}
