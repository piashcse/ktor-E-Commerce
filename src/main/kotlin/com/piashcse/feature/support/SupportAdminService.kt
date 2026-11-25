package com.piashcse.feature.support

import com.piashcse.constants.SupportSenderRole
import com.piashcse.constants.TicketPriority
import com.piashcse.constants.TicketStatus
import com.piashcse.database.entities.SupportMessageDAO
import com.piashcse.database.entities.SupportMessageTable
import com.piashcse.database.entities.SupportTicketDAO
import com.piashcse.database.entities.SupportTicketTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.AdminAudit
import com.piashcse.event.OutboxAggregate
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.SendEmailEvent
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.readQuery
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Admin ticket queue, replies, assignment and internal notes. */
class SupportAdminService {
    suspend fun adminTickets(
        status: TicketStatus?,
        priority: TicketPriority?,
        assignedTo: String?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<TicketSummaryResponse> =
        readQuery {
            SupportTicketTable.selectAll()
                .also { query ->
                    status?.let { query.andWhere { SupportTicketTable.status eq it } }
                    priority?.let { query.andWhere { SupportTicketTable.priority eq it } }
                    assignedTo?.let {
                        query.andWhere { SupportTicketTable.assignedTo eq it.entityID(UserTable) }
                    }
                    query.orderBy(SupportTicketTable.lastMessageAt to SortOrder.DESC)
                }.toPaginatedResponse(limit, offset) { SupportTicketDAO.wrapRow(it).toSummary() }
        }

    suspend fun adminTicket(ticketId: String): TicketResponse =
        readQuery {
            val ticket = SupportTicketDAO.findById(ticketId) ?: ticketId.throwNotFound("Ticket")
            TicketResponse(
                ticket = ticket.toSummary(),
                messages =
                    SupportMessageDAO.find { SupportMessageTable.ticketId eq ticket.id }
                        .orderBy(SupportMessageTable.createdAt to SortOrder.ASC)
                        .map { it.toResponse() },
                attachments = ticketAttachments(ticket.id.value),
            )
        }

    suspend fun adminReply(
        adminId: String,
        ticketId: String,
        request: TicketReplyRequest,
    ): TicketResponse =
        query {
            val ticket = SupportTicketDAO.findById(ticketId) ?: ticketId.throwNotFound("Ticket")
            val now = currentUtc()
            SupportMessageDAO.new {
                this.ticketId = ticket.id
                this.senderId = adminId.entityID(UserTable)
                this.senderRole = SupportSenderRole.ADMIN
                body = request.body
                isInternal = request.isInternal
            }
            if (!request.isInternal && ticket.status in listOf(TicketStatus.PENDING, TicketStatus.RESOLVED)) {
                ticket.status = TicketStatus.OPEN
                ticket.resolvedAt = null
            }
            ticket.lastMessageAt = now
            if (!request.isInternal && ticket.firstResponseAt == null) ticket.firstResponseAt = now
            AdminAudit.publish(
                adminId,
                if (request.isInternal) "SUPPORT_INTERNAL_NOTE" else "SUPPORT_REPLY",
                "SUPPORT_TICKET",
                ticket.id.value,
            )
            if (!request.isInternal) {
                UserDAO.findById(ticket.userId.value)?.email?.takeIf { it.isNotBlank() }?.let { email ->
                    OutboxPublisher.enqueueTx(
                        OutboxAggregate.SUPPORT,
                        ticket.id.value,
                        SendEmailEvent(
                            to = email,
                            subject = "Support reply: ${ticket.subject}",
                            body = request.body,
                        ),
                    )
                }
            }
            adminTicketTx(ticket)
        }

    suspend fun adminUpdate(
        ticketId: String,
        request: TicketUpdateRequest,
        adminId: String? = null,
    ): TicketSummaryResponse =
        query {
            val ticket = SupportTicketDAO.findById(ticketId) ?: ticketId.throwNotFound("Ticket")
            request.status?.let {
                ticket.status = it
                if (it in listOf(TicketStatus.RESOLVED, TicketStatus.CLOSED)) {
                    ticket.resolvedAt = currentUtc()
                }
                if (it == TicketStatus.OPEN) ticket.resolvedAt = null
            }
            request.priority?.let { ticket.priority = it }
            request.category?.let { ticket.category = it }
            request.assignedTo?.let { assigned ->
                if (assigned.isBlank()) {
                    ticket.assignedTo = null
                } else {
                    requireUser(assigned)
                    ticket.assignedTo = assigned.entityID(UserTable)
                }
            }
            adminId?.let {
                AdminAudit.publish(
                    it,
                    "SUPPORT_TICKET_UPDATED",
                    "SUPPORT_TICKET",
                    ticket.id.value,
                    "Status ${ticket.status}, priority ${ticket.priority}, assigned ${ticket.assignedTo?.value.orEmpty()}",
                )
            }
            ticket.toSummary()
        }

    suspend fun attachmentForAdmin(
        ticketId: String,
        attachmentId: String,
    ): SupportAttachmentDownload =
        readQuery {
            val ticket = SupportTicketDAO.findById(ticketId) ?: ticketId.throwNotFound("Ticket")
            findAttachment(ticket.id.value, attachmentId)
        }

    private fun adminTicketTx(ticket: SupportTicketDAO): TicketResponse =
        TicketResponse(
            ticket = ticket.toSummary(),
            messages =
                SupportMessageDAO.find { SupportMessageTable.ticketId eq ticket.id }
                    .orderBy(SupportMessageTable.createdAt to SortOrder.ASC)
                    .map { it.toResponse() },
            attachments = ticketAttachments(ticket.id.value),
        )
}
