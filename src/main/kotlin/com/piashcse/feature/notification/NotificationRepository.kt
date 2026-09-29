package com.piashcse.feature.notification

import com.piashcse.utils.common.PaginatedResponse
import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val id: String,
    val channel: String,
    val type: String,
    val title: String,
    val body: String?,
    val resourceType: String?,
    val resourceId: String?,
    val isRead: Boolean,
    val createdAt: String,
)

@Serializable
data class UnreadCountResponse(val unreadCount: Long)

interface NotificationRepository {
    suspend fun list(userId: String, limit: Int, offset: Int, unreadOnly: Boolean = false): PaginatedResponse<NotificationResponse>
    suspend fun unreadCount(userId: String): Long
    suspend fun markRead(userId: String, notificationId: String): NotificationResponse
    suspend fun markAllRead(userId: String): Long
}
