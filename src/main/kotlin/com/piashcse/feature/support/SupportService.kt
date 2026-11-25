package com.piashcse.feature.support

import com.piashcse.config.DotEnvConfig
import com.piashcse.constants.SupportSenderRole
import com.piashcse.constants.TicketStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.SupportAttachmentDAO
import com.piashcse.database.entities.SupportMessageDAO
import com.piashcse.database.entities.SupportMessageTable
import com.piashcse.database.entities.SupportTicketDAO
import com.piashcse.database.entities.SupportTicketTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.OutboxAggregate
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.SendEmailEvent
import com.piashcse.service.AppMetrics
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.readQuery
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Customer-facing support tickets, replies, ratings and attachments. */
class SupportService {
    suspend fun create(
        userId: String,
        request: CreateTicketRequest,
    ): TicketResponse =
        query {
            requireUser(userId)
            val order =
                request.orderId?.let { orderId ->
                    val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
                    if (order.userId.value != userId) throw ForbiddenException("Order belongs to another account")
                    order
                }
            val now = currentUtc()
            val ticket =
                SupportTicketDAO.new {
                    this.userId = userId.entityID(UserTable)
                    this.orderId = order?.id
                    this.shopId = order?.shopId
                    subject = request.subject
                    category = request.category
                    priority = request.priority
                    status = TicketStatus.OPEN
                    lastMessageAt = now
                }
            val message =
                SupportMessageDAO.new {
                    this.ticketId = ticket.id
                    this.senderId = userId.entityID(UserTable)
                    this.senderRole = SupportSenderRole.CUSTOMER
                    body = request.body
                    isInternal = false
                }
            AppMetrics.supportTicketsOpened.increment()
            OutboxPublisher.enqueueTx(
                OutboxAggregate.SUPPORT,
                ticket.id.value,
                SendEmailEvent(
                    to = DotEnvConfig.supportEmail,
                    subject = "New support ticket: ${ticket.subject}",
                    body = "Ticket ${ticket.id.value} (${ticket.priority}) was opened by $userId.",
                ),
            )
            TicketResponse(ticket.toSummary(), listOf(message.toResponse()))
        }

    suspend fun myTickets(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<TicketSummaryResponse> =
        readQuery {
            SupportTicketTable.selectAll()
                .where { SupportTicketTable.userId eq userId.entityID(UserTable) }
                .orderBy(SupportTicketTable.lastMessageAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { SupportTicketDAO.wrapRow(it).toSummary() }
        }

    suspend fun myTicket(
        userId: String,
        ticketId: String,
    ): TicketResponse =
        readQuery {
            val ticket = SupportTicketDAO.findById(ticketId) ?: ticketId.throwNotFound("Ticket")
            if (ticket.userId.value != userId) throw ForbiddenException("Ticket belongs to another account")
            TicketResponse(
                ticket = ticket.toSummary(),
                messages =
                    SupportMessageDAO.find {
                        (SupportMessageTable.ticketId eq ticket.id) and (SupportMessageTable.isInternal eq false)
                    }.orderBy(SupportMessageTable.createdAt to SortOrder.ASC)
                        .map { it.toResponse() },
                attachments = ticketAttachments(ticket.id.value),
            )
        }

    suspend fun reply(
        userId: String,
        ticketId: String,
        request: TicketReplyRequest,
    ): TicketResponse =
        query {
            val ticket = SupportTicketDAO.findById(ticketId) ?: ticketId.throwNotFound("Ticket")
            if (ticket.userId.value != userId) throw ForbiddenException("Ticket belongs to another account")
            if (ticket.status == TicketStatus.CLOSED) throw ValidationException("Ticket is closed")
            val now = currentUtc()
            SupportMessageDAO.new {
                this.ticketId = ticket.id
                this.senderId = userId.entityID(UserTable)
                this.senderRole = SupportSenderRole.CUSTOMER
                body = request.body
                isInternal = false
            }
            if (ticket.status in listOf(TicketStatus.PENDING, TicketStatus.RESOLVED)) {
                ticket.status = TicketStatus.OPEN
                ticket.resolvedAt = null
            }
            ticket.lastMessageAt = now
            OutboxPublisher.enqueueTx(
                OutboxAggregate.SUPPORT,
                ticket.id.value,
                SendEmailEvent(
                    to = DotEnvConfig.supportEmail,
                    subject = "Customer replied to ticket ${ticket.id.value}",
                    body = "Ticket ${ticket.id.value}: ${request.body.take(500)}",
                ),
            )
            myTicketTx(ticket)
        }

    suspend fun close(
        userId: String,
        ticketId: String,
    ): TicketSummaryResponse =
        query {
            val ticket = SupportTicketDAO.findById(ticketId) ?: ticketId.throwNotFound("Ticket")
            if (ticket.userId.value != userId) throw ForbiddenException("Ticket belongs to another account")
            ticket.status = TicketStatus.CLOSED
            ticket.resolvedAt = currentUtc()
            ticket.toSummary()
        }

    suspend fun rate(
        userId: String,
        ticketId: String,
        request: TicketSatisfactionRequest,
    ): TicketSummaryResponse =
        query {
            val ticket = SupportTicketDAO.findById(ticketId) ?: ticketId.throwNotFound("Ticket")
            if (ticket.userId.value != userId) throw ForbiddenException("Ticket belongs to another account")
            if (ticket.status !in listOf(TicketStatus.RESOLVED, TicketStatus.CLOSED)) {
                throw ValidationException("Only resolved or closed tickets can be rated")
            }
            if (ticket.csatRatedAt != null) {
                throw ValidationException("This ticket has already been rated")
            }
            ticket.csatScore = request.score
            ticket.csatComment = request.comment?.trim()?.takeIf { it.isNotBlank() }
            ticket.csatRatedAt = currentUtc()
            ticket.toSummary()
        }

    suspend fun attach(
        userId: String,
        ticketId: String,
        fileName: String,
        contentType: String?,
        storageName: String,
        sizeBytes: Long,
        isAdmin: Boolean = false,
    ): TicketAttachmentResponse =
        query {
            val ticket = SupportTicketDAO.findById(ticketId) ?: ticketId.throwNotFound("Ticket")
            if (!isAdmin && ticket.userId.value != userId) {
                throw ForbiddenException("Ticket belongs to another account")
            }
            SupportAttachmentDAO.new {
                this.ticketId = ticket.id
                this.uploadedBy = userId.entityID(UserTable)
                this.fileName = fileName.take(255)
                this.contentType = contentType?.take(100)
                this.storageName = storageName
                this.sizeBytes = sizeBytes
            }.toAttachmentResponse()
        }

    suspend fun attachmentForCustomer(
        userId: String,
        ticketId: String,
        attachmentId: String,
    ): SupportAttachmentDownload =
        readQuery {
            val ticket = SupportTicketDAO.findById(ticketId) ?: ticketId.throwNotFound("Ticket")
            if (ticket.userId.value != userId) throw ForbiddenException("Ticket belongs to another account")
            findAttachment(ticket.id.value, attachmentId)
        }

    private fun myTicketTx(ticket: SupportTicketDAO): TicketResponse =
        TicketResponse(
            ticket = ticket.toSummary(),
            messages =
                SupportMessageDAO.find {
                    (SupportMessageTable.ticketId eq ticket.id) and
                        (SupportMessageTable.isInternal eq false)
                }.orderBy(SupportMessageTable.createdAt to SortOrder.ASC)
                    .map { it.toResponse() },
            attachments = ticketAttachments(ticket.id.value),
        )
}
