package com.piashcse.feature.notification

import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class MarkAllReadResponse(val markedRead: Long)

@Serializable
data class SendNotificationRequest(
    val userId: String,
    val title: String,
    val body: String,
    val channel: String? = null,
    val type: String? = null,
) {
    init {
        validate(this) {
            validate(SendNotificationRequest::userId).isNotNull().isNotEmpty()
            validate(SendNotificationRequest::title).isNotNull().isNotEmpty()
            validate(SendNotificationRequest::body).isNotNull().isNotEmpty()
        }
    }
}

@Serializable
data class UpdateNotificationPreferencesRequest(
    val channel: String,
) {
    init {
        validate(this) {
            validate(UpdateNotificationPreferencesRequest::channel).isNotNull().isNotEmpty()
        }
    }
}
