package com.piashcse.feature.support

import com.piashcse.constants.SupportSenderRole
import com.piashcse.constants.TicketCategory
import com.piashcse.constants.TicketPriority
import com.piashcse.constants.TicketStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.SupportMessageDAO
import com.piashcse.database.entities.SupportMessageTable
import com.piashcse.database.entities.SupportTicketDAO
import com.piashcse.database.entities.SupportTicketTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.AdminAudit
import com.piashcse.service.AppMetrics
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

@Serializable
data class CreateTicketRequest(
    val subject: String,
    val body: String,
    val category: TicketCategory = TicketCategory.OTHER,
    val priority: TicketPriority = TicketPriority.NORMAL,
    val orderId: String? = null,
) {
    init {
        val violations =
            buildList {
                if (subject.isBlank()) add(FieldViolation("subject", "Subject is required"))
                if (body.isBlank()) add(FieldViolation("body", "Message body is required"))
            }
        if (violations.isNotEmpty()) throw FieldValidationException(violations)
    }
}

@Serializable
data class TicketReplyRequest(
    val body: String,
    val isInternal: Boolean = false,
) {
    init {
        if (body.isBlank()) throw FieldValidationException(listOf(FieldViolation("body", "Message body is required")))
    }
}

@Serializable
data class TicketUpdateRequest(
    val status: TicketStatus? = null,
    val priority: TicketPriority? = null,
    val category: TicketCategory? = null,
    val assignedTo: String? = null,
)

@Serializable
data class TicketSatisfactionRequest(
    val score: Int,
    val comment: String? = null,
) {
    init {
        if (score !in 1..5) {
            throw FieldValidationException(listOf(FieldViolation("score", "Score must be between 1 and 5")))
        }
    }
}

@Serializable
data class TicketMessageResponse(
    val id: String,
    val ticketId: String,
    val senderId: String,
    val senderRole: SupportSenderRole,
    val body: String,
    val isInternal: Boolean,
    val createdAt: String?,
)

@Serializable
data class TicketSummaryResponse(
    val id: String,
    val userId: String,
    val orderId: String?,
    val shopId: String?,
    val subject: String,
    val category: TicketCategory,
    val priority: TicketPriority,
    val status: TicketStatus,
    val assignedTo: String?,
    val lastMessageAt: String?,
    val resolvedAt: String?,
    val createdAt: String?,
    val firstResponseAt: String? = null,
    val responseDueAt: String? = null,
    val resolutionDueAt: String? = null,
    val slaBreached: Boolean = false,
    val csatScore: Int? = null,
    val csatComment: String? = null,
    val csatRatedAt: String? = null,
)

@Serializable
data class TicketResponse(
    val ticket: TicketSummaryResponse,
    val messages: List<TicketMessageResponse>,
)

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
            TicketResponse(ticket.toSummary(), listOf(message.toResponse()))
        }

    suspend fun myTickets(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<TicketSummaryResponse> =
        query {
            SupportTicketTable.selectAll()
                .where { SupportTicketTable.userId eq userId.entityID(UserTable) }
                .orderBy(SupportTicketTable.lastMessageAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { SupportTicketDAO.wrapRow(it).toSummary() }
        }

    suspend fun myTicket(
        userId: String,
        ticketId: String,
    ): TicketResponse =
        query {
            val ticket = SupportTicketDAO.findById(ticketId) ?: ticketId.throwNotFound("Ticket")
            if (ticket.userId.value != userId) throw ForbiddenException("Ticket belongs to another account")
            TicketResponse(
                ticket = ticket.toSummary(),
                messages =
                    SupportMessageDAO.find {
                        (SupportMessageTable.ticketId eq ticket.id) and (SupportMessageTable.isInternal eq false)
                    }.orderBy(SupportMessageTable.createdAt to SortOrder.ASC)
                        .map { it.toResponse() },
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
            ticket.csatScore = request.score
            ticket.csatComment = request.comment?.trim()?.takeIf { it.isNotBlank() }
            ticket.csatRatedAt = currentUtc()
            ticket.toSummary()
        }

    suspend fun adminTickets(
        status: TicketStatus?,
        priority: TicketPriority?,
        assignedTo: String?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<TicketSummaryResponse> =
        query {
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
        query {
            val ticket = SupportTicketDAO.findById(ticketId) ?: ticketId.throwNotFound("Ticket")
            TicketResponse(
                ticket = ticket.toSummary(),
                messages =
                    SupportMessageDAO.find { SupportMessageTable.ticketId eq ticket.id }
                        .orderBy(SupportMessageTable.createdAt to SortOrder.ASC)
                        .map { it.toResponse() },
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
            if (ticket.firstResponseAt == null) ticket.firstResponseAt = now
            AdminAudit.publish(
                adminId,
                if (request.isInternal) "SUPPORT_INTERNAL_NOTE" else "SUPPORT_REPLY",
                "SUPPORT_TICKET",
                ticket.id.value,
            )
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

    private fun adminTicketTx(ticket: SupportTicketDAO): TicketResponse =
        TicketResponse(
            ticket = ticket.toSummary(),
            messages =
                SupportMessageDAO.find { SupportMessageTable.ticketId eq ticket.id }
                    .orderBy(SupportMessageTable.createdAt to SortOrder.ASC)
                    .map { it.toResponse() },
        )

    private fun myTicketTx(ticket: SupportTicketDAO): TicketResponse =
        TicketResponse(
            ticket = ticket.toSummary(),
            messages =
                SupportMessageDAO.find {
                    (SupportMessageTable.ticketId eq ticket.id) and
                        (SupportMessageTable.isInternal eq false)
                }.orderBy(SupportMessageTable.createdAt to SortOrder.ASC)
                    .map { it.toResponse() },
        )

    private fun SupportTicketDAO.responseDueAt() = createdAt.plusHours(responseHours())

    private fun SupportTicketDAO.resolutionDueAt() = createdAt.plusHours(resolutionHours())

    private fun SupportTicketDAO.responseHours(): Long =
        when (priority) {
            TicketPriority.URGENT -> 1
            TicketPriority.HIGH -> 4
            TicketPriority.NORMAL -> 24
            TicketPriority.LOW -> 48
        }

    private fun SupportTicketDAO.resolutionHours(): Long =
        when (priority) {
            TicketPriority.URGENT -> 4
            TicketPriority.HIGH -> 24
            TicketPriority.NORMAL -> 72
            TicketPriority.LOW -> 120
        }

    private fun SupportTicketDAO.slaBreached(): Boolean {
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

    private fun requireUser(userId: String) {
        UserDAO.findById(userId) ?: userId.throwNotFound("User")
    }

    private fun SupportTicketDAO.toSummary() =
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

    private fun SupportMessageDAO.toResponse() =
        TicketMessageResponse(
            id = id.value,
            ticketId = ticketId.value,
            senderId = senderId.value,
            senderRole = senderRole,
            body = body,
            isInternal = isInternal,
            createdAt = createdAt.toUtcString(),
        )
}
