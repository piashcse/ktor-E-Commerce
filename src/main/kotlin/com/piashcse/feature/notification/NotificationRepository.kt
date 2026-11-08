package com.piashcse.feature.notification

import com.piashcse.constants.UserType
import com.piashcse.utils.common.CursorPage
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

@Serializable
data class NotificationPreferencesResponse(
    val userId: String,
    val channel: String,
)

interface NotificationRepository {
    suspend fun list(
        userId: String,
        limit: Int,
        offset: Int,
        unreadOnly: Boolean = false,
    ): PaginatedResponse<NotificationResponse>

    suspend fun listCursor(
        userId: String,
        limit: Int,
        cursor: String?,
        unreadOnly: Boolean = false,
    ): CursorPage<NotificationResponse>

    suspend fun unreadCount(userId: String): Long

    suspend fun markRead(
        userId: String,
        notificationId: String,
    ): NotificationResponse

    suspend fun markAllRead(userId: String): Long

    /**
     * Sends a notification. Sellers may only target users who have ordered from
     * their shop; admins may target anyone. The reserved preference type is
     * rejected so it cannot be forged through this endpoint.
     */
    suspend fun send(
        actorUserId: String,
        actorType: UserType?,
        userId: String,
        title: String,
        body: String,
        channel: String?,
        type: String?,
    ): NotificationResponse

    suspend fun getPreferences(userId: String): NotificationPreferencesResponse

    suspend fun updatePreferences(
        userId: String,
        channel: String,
    ): NotificationPreferencesResponse
}
