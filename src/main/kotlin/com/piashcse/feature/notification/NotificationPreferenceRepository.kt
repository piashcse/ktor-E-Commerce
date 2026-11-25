package com.piashcse.feature.notification

import com.piashcse.database.entities.NotificationDAO
import com.piashcse.database.entities.NotificationPreferenceDAO
import com.piashcse.database.entities.NotificationPreferenceTable
import com.piashcse.database.entities.NotificationTable
import com.piashcse.database.entities.UserTable
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

interface NotificationPreferenceRepository {
    suspend fun override(
        userId: String,
        eventType: String,
    ): String?

    suspend fun defaultChannel(userId: String): String?

    suspend fun listOverrides(userId: String): List<NotificationPreferenceResponse>

    suspend fun upsert(
        userId: String,
        eventType: String,
        channel: String,
    ): NotificationPreferenceResponse
}

class NotificationPreferenceRepositoryImpl : NotificationPreferenceRepository {
    override suspend fun override(
        userId: String,
        eventType: String,
    ): String? =
        query {
            NotificationPreferenceDAO.find {
                (NotificationPreferenceTable.userId eq userId.entityID(UserTable)) and
                    (NotificationPreferenceTable.eventType eq eventType)
            }.firstOrNull()?.channel
        }

    override suspend fun defaultChannel(userId: String): String? =
        query {
            NotificationDAO.find {
                (NotificationTable.userId eq userId.entityID(UserTable)) and
                    (NotificationTable.type eq "NOTIFICATION_PREFS")
            }.orderBy(NotificationTable.createdAt to SortOrder.DESC).firstOrNull()?.channel
        }

    override suspend fun listOverrides(userId: String): List<NotificationPreferenceResponse> =
        query {
            NotificationPreferenceDAO.find {
                NotificationPreferenceTable.userId eq userId.entityID(UserTable)
            }.orderBy(NotificationPreferenceTable.eventType to SortOrder.ASC)
                .map { NotificationPreferenceResponse(it.eventType, it.channel) }
        }

    override suspend fun upsert(
        userId: String,
        eventType: String,
        channel: String,
    ): NotificationPreferenceResponse =
        query {
            val id = userId.entityID(UserTable)
            val existing =
                NotificationPreferenceDAO.find {
                    (NotificationPreferenceTable.userId eq id) and
                        (NotificationPreferenceTable.eventType eq eventType)
                }.firstOrNull()
            if (existing != null) {
                existing.channel = channel
                NotificationPreferenceResponse(existing.eventType, existing.channel)
            } else {
                NotificationPreferenceDAO.new {
                    this.userId = id
                    this.eventType = eventType
                    this.channel = channel
                }.let { NotificationPreferenceResponse(it.eventType, it.channel) }
            }
        }
}
