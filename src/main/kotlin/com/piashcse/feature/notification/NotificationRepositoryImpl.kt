package com.piashcse.feature.notification

import com.piashcse.database.entities.NotificationDAO
import com.piashcse.database.entities.NotificationTable
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class NotificationRepositoryImpl : NotificationRepository {
    private fun NotificationDAO.toResponse() = NotificationResponse(
        id = id.value,
        channel = channel,
        type = type,
        title = title,
        body = body,
        resourceType = resourceType,
        resourceId = resourceId,
        isRead = isRead,
        createdAt = createdAt.toString(),
    )

    override suspend fun list(userId: String, limit: Int, offset: Int, unreadOnly: Boolean) = query {
        NotificationTable.selectAll().andWhere { NotificationTable.userId eq userId }
            .also { q -> if (unreadOnly) q.andWhere { NotificationTable.isRead eq false } }
            .also { q -> q.orderBy(NotificationTable.createdAt to SortOrder.DESC) }
            .toPaginatedResponse(limit, offset) { NotificationDAO.wrapRow(it).toResponse() }
    }

    override suspend fun unreadCount(userId: String): Long = query {
        NotificationDAO.find { (NotificationTable.userId eq userId) and (NotificationTable.isRead eq false) }.count()
    }

    override suspend fun markRead(userId: String, notificationId: String) = query {
        val n = NotificationDAO.findById(notificationId) ?: notificationId.throwNotFound("Notification")
        if (n.userId.value != userId) notificationId.throwNotFound("Notification")
        n.isRead = true
        n.toResponse()
    }

    override suspend fun markAllRead(userId: String): Long = query {
        val unread = NotificationDAO.find { (NotificationTable.userId eq userId) and (NotificationTable.isRead eq false) }.toList()
        unread.forEach { it.isRead = true }
        unread.size.toLong()
    }
}
