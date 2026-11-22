package com.piashcse.feature.refund_request

import com.piashcse.database.entities.RefundRequestDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.event.OutboxAggregate
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.RefundStatusChangedEvent

/** Refund status outbox event shared by lifecycle and provider-reconciliation paths. */
internal fun eventFor(
    refund: RefundRequestDAO,
    fromStatus: String,
    toStatus: String,
) {
    OutboxPublisher.enqueueTx(
        OutboxAggregate.REFUND,
        refund.id.value,
        RefundStatusChangedEvent(
            refundId = refund.id.value,
            orderId = refund.orderId.value,
            userId = refund.userId.value,
            email = UserDAO.findById(refund.userId.value)?.email.orEmpty(),
            fromStatus = fromStatus,
            toStatus = toStatus,
            reason = refund.decisionReason,
        ),
    )
}
