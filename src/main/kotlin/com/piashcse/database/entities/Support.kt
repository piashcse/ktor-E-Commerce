package com.piashcse.database.entities

import com.piashcse.constants.SupportSenderRole
import com.piashcse.constants.TicketCategory
import com.piashcse.constants.TicketPriority
import com.piashcse.constants.TicketStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object SupportTicketTable : BaseIdTable("support_ticket") {
    val userId = reference("user_id", UserTable.id).index()
    val orderId = reference("order_id", OrderTable.id).nullable()
    val shopId = reference("shop_id", ShopTable.id).nullable()
    val subject = varchar("subject", 200)
    val category = enumerationByName("category", 30, TicketCategory::class).default(TicketCategory.OTHER)
    val priority = enumerationByName("priority", 20, TicketPriority::class).default(TicketPriority.NORMAL)
    val status = enumerationByName("status", 20, TicketStatus::class).default(TicketStatus.OPEN)
    val assignedTo = reference("assigned_to", UserTable.id).nullable()
    val lastMessageAt = datetime("last_message_at").nullable()
    val resolvedAt = datetime("resolved_at").nullable()
    val firstResponseAt = datetime("first_response_at").nullable()
    val csatScore = integer("csat_score").nullable()
    val csatComment = text("csat_comment").nullable()
    val csatRatedAt = datetime("csat_rated_at").nullable()
}

class SupportTicketDAO(id: EntityID<String>) : BaseEntity(id, SupportTicketTable) {
    companion object : BaseEntityClass<SupportTicketDAO>(SupportTicketTable, SupportTicketDAO::class.java)

    var userId by SupportTicketTable.userId
    var orderId by SupportTicketTable.orderId
    var shopId by SupportTicketTable.shopId
    var subject by SupportTicketTable.subject
    var category by SupportTicketTable.category
    var priority by SupportTicketTable.priority
    var status by SupportTicketTable.status
    var assignedTo by SupportTicketTable.assignedTo
    var lastMessageAt by SupportTicketTable.lastMessageAt
    var resolvedAt by SupportTicketTable.resolvedAt
    var firstResponseAt by SupportTicketTable.firstResponseAt
    var csatScore by SupportTicketTable.csatScore
    var csatComment by SupportTicketTable.csatComment
    var csatRatedAt by SupportTicketTable.csatRatedAt
}

object SupportMessageTable : BaseIdTable("support_message") {
    val ticketId = reference("ticket_id", SupportTicketTable.id).index()
    val senderId = reference("sender_id", UserTable.id)
    val senderRole = enumerationByName("sender_role", 20, SupportSenderRole::class)
    val body = text("body")
    val isInternal = bool("is_internal").default(false)
}

class SupportMessageDAO(id: EntityID<String>) : BaseEntity(id, SupportMessageTable) {
    companion object : BaseEntityClass<SupportMessageDAO>(SupportMessageTable, SupportMessageDAO::class.java)

    var ticketId by SupportMessageTable.ticketId
    var senderId by SupportMessageTable.senderId
    var senderRole by SupportMessageTable.senderRole
    var body by SupportMessageTable.body
    var isInternal by SupportMessageTable.isInternal
}

object SupportAttachmentTable : BaseIdTable("support_attachment") {
    val ticketId = reference("ticket_id", SupportTicketTable.id).index()
    val uploadedBy = reference("uploaded_by", UserTable.id)
    val fileName = varchar("file_name", 255)
    val contentType = varchar("content_type", 100).nullable()
    val storageName = varchar("storage_name", 255)
    val sizeBytes = long("size_bytes").default(0)
}

class SupportAttachmentDAO(id: EntityID<String>) : BaseEntity(id, SupportAttachmentTable) {
    companion object : BaseEntityClass<SupportAttachmentDAO>(SupportAttachmentTable, SupportAttachmentDAO::class.java)

    var ticketId by SupportAttachmentTable.ticketId
    var uploadedBy by SupportAttachmentTable.uploadedBy
    var fileName by SupportAttachmentTable.fileName
    var contentType by SupportAttachmentTable.contentType
    var storageName by SupportAttachmentTable.storageName
    var sizeBytes by SupportAttachmentTable.sizeBytes
}
