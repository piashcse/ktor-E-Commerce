package com.piashcse.feature.notification

import com.piashcse.database.entities.NotificationDAO
import com.piashcse.database.entities.NotificationPreferenceDAO
import com.piashcse.database.entities.NotificationPreferenceTable
import com.piashcse.database.entities.NotificationTable
import com.piashcse.database.entities.UserTable
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

@Serializable
data class NotificationPreferenceResponse(
    val eventType: String,
    val channel: String,
)

@Serializable
data class NotificationPreferencesListResponse(
    val defaultChannel: String,
    val overrides: List<NotificationPreferenceResponse>,
)

class NotificationPreferencesService {
    private val allowedChannels = setOf("IN_APP", "EMAIL", "PUSH", "SMS")

    suspend fun resolve(
        userId: String,
        eventType: String,
    ): String? =
        query {
            NotificationPreferenceDAO.find {
                (NotificationPreferenceTable.userId eq userId.entityID(UserTable)) and
                    (NotificationPreferenceTable.eventType eq eventType)
            }.firstOrNull()?.channel
                ?: NotificationDAO.find {
                    (NotificationTable.userId eq userId.entityID(UserTable)) and
                        (NotificationTable.type eq "NOTIFICATION_PREFS")
                }.orderBy(NotificationTable.createdAt to SortOrder.DESC).firstOrNull()?.channel
        }

    suspend fun get(
        userId: String,
        eventType: String,
    ): NotificationPreferenceResponse {
        val normalized = eventType.uppercase()
        val override =
            query {
                NotificationPreferenceDAO.find {
                    (NotificationPreferenceTable.userId eq userId.entityID(UserTable)) and
                        (NotificationPreferenceTable.eventType eq normalized)
                }.firstOrNull()?.channel
            }
        val channel = override ?: resolve(userId, normalized) ?: "IN_APP"
        return NotificationPreferenceResponse(normalized, channel)
    }

    suspend fun list(userId: String): NotificationPreferencesListResponse =
        query {
            val default =
                NotificationDAO.find {
                    (NotificationTable.userId eq userId.entityID(UserTable)) and
                        (NotificationTable.type eq "NOTIFICATION_PREFS")
                }.orderBy(NotificationTable.createdAt to SortOrder.DESC).firstOrNull()?.channel ?: "IN_APP"
            val overrides =
                NotificationPreferenceDAO.find {
                    NotificationPreferenceTable.userId eq userId.entityID(UserTable)
                }.orderBy(NotificationPreferenceTable.eventType to SortOrder.ASC)
                    .map { NotificationPreferenceResponse(it.eventType, it.channel) }
            NotificationPreferencesListResponse(default, overrides)
        }

    suspend fun set(
        userId: String,
        eventType: String,
        channel: String,
    ): NotificationPreferenceResponse =
        query {
            if (eventType.isBlank()) throw ValidationException("eventType is required")
            val normalized = channel.uppercase()
            if (normalized !in allowedChannels) throw ValidationException("Invalid channel: $channel")
            val id = userId.entityID(UserTable)
            val existing =
                NotificationPreferenceDAO.find {
                    (NotificationPreferenceTable.userId eq id) and
                        (NotificationPreferenceTable.eventType eq eventType.uppercase())
                }.firstOrNull()
            if (existing != null) {
                existing.channel = normalized
                NotificationPreferenceResponse(existing.eventType, existing.channel)
            } else {
                NotificationPreferenceDAO.new {
                    this.userId = id
                    this.eventType = eventType.uppercase()
                    this.channel = normalized
                }.let { NotificationPreferenceResponse(it.eventType, it.channel) }
            }
        }
}
