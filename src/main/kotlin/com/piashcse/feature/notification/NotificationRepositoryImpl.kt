package com.piashcse.feature.notification

import com.piashcse.database.entities.NotificationDAO
import com.piashcse.database.entities.NotificationTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class NotificationRepositoryImpl : NotificationRepository {
    private fun NotificationDAO.toResponse() =
        NotificationResponse(
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

    companion object {
        const val DEFAULT_CHANNEL = "IN_APP"
        const val DEFAULT_SEND_TYPE = "ANNOUNCEMENT"
        private const val PREFS_TYPE = "NOTIFICATION_PREFS"
        private val ALLOWED_CHANNELS = setOf("IN_APP", "EMAIL", "PUSH", "SMS")
    }

    private fun requireChannel(channel: String): String {
        val normalized = channel.uppercase()
        if (normalized !in ALLOWED_CHANNELS) {
            throw ValidationException("Invalid channel: $channel. Allowed: ${ALLOWED_CHANNELS.joinToString(", ")}")
        }
        return normalized
    }

    override suspend fun list(
        userId: String,
        limit: Int,
        offset: Int,
        unreadOnly: Boolean,
    ) = query {
        NotificationTable.selectAll().andWhere { NotificationTable.userId eq userId }
            .also { q ->
                // Preference rows share this table (no extra migration); they are
                // never user-visible notifications.
                q.andWhere { NotificationTable.type neq PREFS_TYPE }
                if (unreadOnly) q.andWhere { NotificationTable.isRead eq false }
            }
            .also { q -> q.orderBy(NotificationTable.createdAt to SortOrder.DESC) }
            .toPaginatedResponse(limit, offset) { NotificationDAO.wrapRow(it).toResponse() }
    }

    override suspend fun unreadCount(userId: String): Long =
        query {
            NotificationDAO.find {
                (NotificationTable.userId eq userId) and
                    (NotificationTable.isRead eq false) and
                    (NotificationTable.type neq PREFS_TYPE)
            }.count()
        }

    override suspend fun markRead(
        userId: String,
        notificationId: String,
    ) = query {
        val n = NotificationDAO.findById(notificationId) ?: notificationId.throwNotFound("Notification")
        if (n.userId.value != userId) notificationId.throwNotFound("Notification")
        n.isRead = true
        n.toResponse()
    }

    override suspend fun markAllRead(userId: String): Long =
        query {
            val unread =
                NotificationDAO.find {
                    (NotificationTable.userId eq userId) and
                        (NotificationTable.isRead eq false) and
                        (NotificationTable.type neq PREFS_TYPE)
                }.toList()
            unread.forEach { it.isRead = true }
            unread.size.toLong()
        }

    override suspend fun send(
        userId: String,
        title: String,
        body: String,
        channel: String?,
        type: String?,
    ) = query {
        UserDAO.findById(userId) ?: userId.throwNotFound("User")
        if (title.isBlank()) throw ValidationException("Notification title cannot be blank")
        if (body.isBlank()) throw ValidationException("Notification body cannot be blank")
        val resolvedChannel = channel?.let { requireChannel(it) } ?: storedChannel(userId)
        NotificationDAO.new {
            this.userId = userId.entityID(UserTable)
            this.channel = resolvedChannel
            this.type = type?.takeIf { it.isNotBlank() }?.uppercase() ?: DEFAULT_SEND_TYPE
            this.title = title
            this.body = body
        }.toResponse()
    }

    override suspend fun getPreferences(userId: String) =
        query {
            NotificationPreferencesResponse(userId, storedChannel(userId))
        }

    override suspend fun updatePreferences(
        userId: String,
        channel: String,
    ) = query {
        UserDAO.findById(userId) ?: userId.throwNotFound("User")
        val normalized = requireChannel(channel)
        // Channel prefs reuse the notification table (append-only history, no new
        // table/migration); new users default to IN_APP in code. Readers must
        // filter PREFS_TYPE rows out of user-visible queries (see list/unreadCount).
        NotificationDAO.new {
            this.userId = userId.entityID(UserTable)
            this.channel = normalized
            this.type = PREFS_TYPE
            this.title = "Notification channel preference"
            this.body = normalized
            this.isRead = true
        }
        NotificationPreferencesResponse(userId, normalized)
    }

    private fun storedChannel(userId: String): String =
        NotificationDAO.find {
            (NotificationTable.userId eq userId) and (NotificationTable.type eq PREFS_TYPE)
        }.orderBy(NotificationTable.createdAt to SortOrder.DESC).firstOrNull()?.channel
            ?: DEFAULT_CHANNEL
}
