package com.piashcse.feature.rma

import com.piashcse.constants.RmaStatus
import com.piashcse.database.entities.*
import com.piashcse.event.OutboxAggregate
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.RmaStatusChangedEvent
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.*

internal fun RmaDAO.toRmaResponse() =
    RmaResponse(
        id = id.value,
        orderId = orderId.value,
        orderItemId = orderItemId.value,
        userId = userId.value,
        shopId = shopId?.value,
        quantity = quantity,
        type = type,
        status = status,
        reason = reason,
        resolution = resolution,
        note = resolutionNote,
        allowedActions =
            RmaStatus.entries
                .filter { it != RmaStatus.IN_TRANSIT && it != RmaStatus.CANCELLED }
                .filter { RmaStatus.canTransitionTo(status, it) }
                .map { it.name },
        trackingNumber = trackingNumber,
        replacementOrderId = replacementOrderId?.value,
        refundRequestId = refundRequestId?.value,
        createdAt = createdAt.toUtcString(),
        resolvedAt = resolvedAt?.toUtcString(),
    )

internal object RmaEvents {
    fun enqueueStatusChange(
        rma: RmaDAO,
        fromStatus: RmaStatus?,
        toStatus: RmaStatus,
    ) {
        if (fromStatus == toStatus) return
        OutboxPublisher.enqueueTx(
            OutboxAggregate.RMA,
            rma.id.value,
            RmaStatusChangedEvent(
                rmaId = rma.id.value,
                orderId = rma.orderId.value,
                userId = rma.userId.value,
                email = UserDAO.findById(rma.userId.value)?.email.orEmpty(),
                fromStatus = fromStatus?.name ?: "NONE",
                toStatus = toStatus.name,
                note = rma.resolutionNote,
            ),
        )
    }
}
