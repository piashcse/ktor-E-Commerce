package com.piashcse.feature.support

import com.piashcse.constants.TicketPriority
import com.piashcse.constants.TicketStatus
import com.piashcse.database.entities.SupportAttachmentDAO
import com.piashcse.database.entities.SupportAttachmentTable
import com.piashcse.database.entities.SupportMessageDAO
import com.piashcse.database.entities.SupportTicketDAO
import com.piashcse.database.entities.SupportTicketTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.throwNotFound
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

internal fun findAttachment(
    ticketId: String,
    attachmentId: String,
): SupportAttachmentDownload {
    val attachment =
        SupportAttachmentDAO.find {
            (SupportAttachmentTable.id eq attachmentId.entityID(SupportAttachmentTable)) and
                (SupportAttachmentTable.ticketId eq ticketId.entityID(SupportTicketTable))
        }.firstOrNull() ?: attachmentId.throwNotFound("Attachment")
    return SupportAttachmentDownload(attachment.storageName, attachment.fileName, attachment.contentType)
}

internal fun ticketAttachments(ticketId: String): List<TicketAttachmentResponse> =
    SupportAttachmentDAO.find { SupportAttachmentTable.ticketId eq ticketId.entityID(SupportTicketTable) }
        .orderBy(SupportAttachmentTable.createdAt to SortOrder.ASC)
        .map { it.toAttachmentResponse() }

internal fun SupportAttachmentDAO.toAttachmentResponse() =
    TicketAttachmentResponse(
        id = id.value,
        ticketId = ticketId.value,
        fileName = fileName,
        contentType = contentType,
        sizeBytes = sizeBytes,
        createdAt = createdAt.toUtcString(),
    )

internal fun SupportTicketDAO.responseDueAt() = createdAt.plusHours(responseHours())

internal fun SupportTicketDAO.resolutionDueAt() = createdAt.plusHours(resolutionHours())

internal fun SupportTicketDAO.responseHours(): Long =
    when (priority) {
        TicketPriority.URGENT -> 1
        TicketPriority.HIGH -> 4
        TicketPriority.NORMAL -> 24
        TicketPriority.LOW -> 48
    }

internal fun SupportTicketDAO.resolutionHours(): Long =
    when (priority) {
        TicketPriority.URGENT -> 4
        TicketPriority.HIGH -> 24
        TicketPriority.NORMAL -> 72
        TicketPriority.LOW -> 120
    }

internal fun SupportTicketDAO.slaBreached(): Boolean {
    val now = currentUtc()
    val responseDeadline = responseDueAt()
    val responseBreached =
        firstResponseAt?.isAfter(responseDeadline) ?: now.isAfter(responseDeadline)
    val resolutionDeadline = resolutionDueAt()
    val resolutionBreached =
        if (status in listOf(TicketStatus.RESOLVED, TicketStatus.CLOSED)) {
            resolvedAt?.isAfter(resolutionDeadline) ?: false
        } else {
            now.isAfter(resolutionDeadline)
        }
    return responseBreached || resolutionBreached
}

internal fun requireUser(userId: String) {
    UserDAO.findById(userId) ?: userId.throwNotFound("User")
}

internal fun SupportTicketDAO.toSummary() =
    TicketSummaryResponse(
        id = id.value,
        userId = userId.value,
        orderId = orderId?.value,
        shopId = shopId?.value,
        subject = subject,
        category = category,
        priority = priority,
        status = status,
        assignedTo = assignedTo?.value,
        lastMessageAt = lastMessageAt?.toUtcString(),
        resolvedAt = resolvedAt?.toUtcString(),
        createdAt = createdAt.toUtcString(),
        firstResponseAt = firstResponseAt?.toUtcString(),
        responseDueAt = responseDueAt().toUtcString(),
        resolutionDueAt = resolutionDueAt().toUtcString(),
        slaBreached = slaBreached(),
        csatScore = csatScore,
        csatComment = csatComment,
        csatRatedAt = csatRatedAt?.toUtcString(),
    )

internal fun SupportMessageDAO.toResponse() =
    TicketMessageResponse(
        id = id.value,
        ticketId = ticketId.value,
        senderId = senderId.value,
        senderRole = senderRole,
        body = body,
        isInternal = isInternal,
        createdAt = createdAt.toUtcString(),
    )
