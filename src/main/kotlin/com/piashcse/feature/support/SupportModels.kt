package com.piashcse.feature.support

import com.piashcse.constants.SupportSenderRole
import com.piashcse.constants.TicketCategory
import com.piashcse.constants.TicketPriority
import com.piashcse.constants.TicketStatus
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import kotlinx.serialization.Serializable

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
data class TicketAttachmentResponse(
    val id: String,
    val ticketId: String,
    val fileName: String,
    val contentType: String?,
    val sizeBytes: Long,
    val createdAt: String?,
)

data class SupportAttachmentDownload(
    val storageName: String,
    val fileName: String,
    val contentType: String?,
)

@Serializable
data class TicketResponse(
    val ticket: TicketSummaryResponse,
    val messages: List<TicketMessageResponse>,
    val attachments: List<TicketAttachmentResponse> = emptyList(),
)
