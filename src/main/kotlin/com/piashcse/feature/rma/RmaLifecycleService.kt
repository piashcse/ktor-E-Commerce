package com.piashcse.feature.rma

import com.piashcse.constants.Message
import com.piashcse.constants.RmaStatus
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException

/** Shipping and cancellation transitions for approved/requested RMAs. */
internal object RmaLifecycleService {
    suspend fun ship(
        rmaId: String,
        userId: String,
        trackingNumber: String,
    ): RmaResponse =
        query {
            val rma = RmaDAO.findById(rmaId) ?: rmaId.throwNotFound("RMA")
            if (rma.userId.value != userId) throw ForbiddenException(Message.Orders.UNAUTHORIZED)
            if (rma.status != RmaStatus.APPROVED) {
                throw ValidationException("RMA must be approved before shipping")
            }
            trackingNumber.requireNotBlank("Tracking number")
            val fromStatus = rma.status
            rma.trackingNumber = trackingNumber
            rma.status = RmaStatus.IN_TRANSIT
            RmaEvents.enqueueStatusChange(rma, fromStatus, rma.status)
            rma.toRmaResponse()
        }

    suspend fun cancel(
        rmaId: String,
        userId: String,
    ): RmaResponse =
        query {
            val rma = RmaDAO.findById(rmaId) ?: rmaId.throwNotFound("RMA")
            if (rma.userId.value != userId) throw ForbiddenException(Message.Orders.UNAUTHORIZED)
            if (rma.status !in listOf(RmaStatus.REQUESTED, RmaStatus.APPROVED)) {
                throw ValidationException("RMA cannot be cancelled from status ${rma.status}")
            }
            val fromStatus = rma.status
            rma.status = RmaStatus.CANCELLED
            rma.resolvedAt = currentUtc()
            RmaEvents.enqueueStatusChange(rma, fromStatus, rma.status)
            rma.toRmaResponse()
        }
}
