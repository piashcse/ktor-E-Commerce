package com.piashcse.feature.notification

import com.piashcse.database.entities.NotificationDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.utils.email.EmailSender
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import org.slf4j.LoggerFactory

data class NotificationMessage(
    val userId: String,
    val email: String?,
    val eventType: String,
    val title: String,
    val body: String,
    val resourceType: String? = null,
    val resourceId: String? = null,
)

interface NotificationProvider {
    val channel: String

    suspend fun send(message: NotificationMessage): Boolean
}

class InAppNotificationProvider : NotificationProvider {
    override val channel = "IN_APP"

    override suspend fun send(message: NotificationMessage): Boolean {
        query {
            NotificationDAO.new {
                userId = message.userId.entityID(UserTable)
                channel = this@InAppNotificationProvider.channel
                type = message.eventType
                title = message.title
                body = message.body
                resourceType = message.resourceType
                resourceId = message.resourceId
            }
        }
        return true
    }
}

class EmailNotificationProvider : NotificationProvider {
    override val channel = "EMAIL"

    override suspend fun send(message: NotificationMessage): Boolean {
        val to = message.email?.takeIf { it.isNotBlank() } ?: return false
        EmailSender.send(to, message.title, message.body)
        return true
    }
}

/**
 * SMS/push adapters are intentionally inert until a provider is configured;
 * they log instead of silently dropping so operators can see traffic.
 */
class SmsNotificationProvider : NotificationProvider {
    private val log = LoggerFactory.getLogger(SmsNotificationProvider::class.java)
    override val channel = "SMS"

    override suspend fun send(message: NotificationMessage): Boolean {
        log.info("SMS provider not configured; skipped ${message.eventType} for user ${message.userId}")
        return false
    }
}

class PushNotificationProvider : NotificationProvider {
    private val log = LoggerFactory.getLogger(PushNotificationProvider::class.java)
    override val channel = "PUSH"

    override suspend fun send(message: NotificationMessage): Boolean {
        log.info("Push provider not configured; skipped ${message.eventType} for user ${message.userId}")
        return false
    }
}

/**
 * Delivers every event to the in-app inbox and to the user's preferred extra
 * channel. IN_APP is the default so the API-reported default and delivery agree.
 */
class NotificationDispatcher(
    private val preferences: NotificationPreferencesService = NotificationPreferencesService(),
) {
    private val providers =
        listOf(
            InAppNotificationProvider(),
            EmailNotificationProvider(),
            SmsNotificationProvider(),
            PushNotificationProvider(),
        ).associateBy { it.channel }

    suspend fun dispatch(
        message: NotificationMessage,
        defaultChannel: String = "IN_APP",
    ) {
        providers["IN_APP"]?.send(message)
        dispatchExternal(message, defaultChannel)
    }

    /** Sends only the user's preferred extra channel (no duplicate in-app row). */
    suspend fun dispatchExternal(
        message: NotificationMessage,
        defaultChannel: String = "IN_APP",
    ) {
        val channel = preferences.resolve(message.userId, message.eventType) ?: defaultChannel
        if (channel != "IN_APP") {
            providers[channel]?.send(message)
        }
    }
}
