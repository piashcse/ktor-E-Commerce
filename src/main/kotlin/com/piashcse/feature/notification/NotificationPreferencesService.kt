package com.piashcse.feature.notification

import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable

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

class NotificationPreferencesService(
    private val repository: NotificationPreferenceRepository,
) {
    private val allowedChannels = setOf("IN_APP", "EMAIL", "PUSH", "SMS")

    suspend fun resolve(
        userId: String,
        eventType: String,
    ): String? = repository.override(userId, eventType) ?: repository.defaultChannel(userId)

    suspend fun get(
        userId: String,
        eventType: String,
    ): NotificationPreferenceResponse {
        val normalized = eventType.uppercase()
        val channel = repository.override(userId, normalized) ?: resolve(userId, normalized) ?: "IN_APP"
        return NotificationPreferenceResponse(normalized, channel)
    }

    suspend fun list(userId: String): NotificationPreferencesListResponse =
        NotificationPreferencesListResponse(
            defaultChannel = repository.defaultChannel(userId) ?: "IN_APP",
            overrides = repository.listOverrides(userId),
        )

    suspend fun set(
        userId: String,
        eventType: String,
        channel: String,
    ): NotificationPreferenceResponse {
        if (eventType.isBlank()) throw ValidationException("eventType is required")
        val normalized = channel.uppercase()
        if (normalized !in allowedChannels) throw ValidationException("Invalid channel: $channel")
        return repository.upsert(userId, eventType.uppercase(), normalized)
    }
}
