package com.piashcse.feature.notification

import com.piashcse.constants.UserType
import com.piashcse.plugin.customerAuth
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
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

fun Route.notificationRoutes() {
    val repo: NotificationRepository by inject()
    customerAuth {
        get {
            val (limit, offset) = call.paginateQueryParams()
            val unreadOnly = call.request.queryParameters["unreadOnly"]?.toBoolean() ?: false
            call.respondOk(repo.list(call.currentUserId, limit, offset, unreadOnly))
        }
        get("unread-count") {
            call.respondOk(UnreadCountResponse(repo.unreadCount(call.currentUserId)))
        }
        post("{id}/read") {
            call.respondOk(repo.markRead(call.currentUserId, call.parameters["id"] ?: throw IllegalArgumentException("id required")))
        }
        post("read-all") {
            call.respondOk(MarkAllReadResponse(repo.markAllRead(call.currentUserId)))
        }
    }
    requireRole(UserType.ADMIN, UserType.SUPER_ADMIN, UserType.SELLER) {
        post("send") {
            val body = call.receive<SendNotificationRequest>()
            call.respondCreated(repo.send(body.userId, body.title, body.body, body.channel, body.type))
        }
    }
    requireRole {
        get("preferences") {
            call.respondOk(repo.getPreferences(call.currentUserId))
        }
        put("preferences") {
            val body = call.receive<UpdateNotificationPreferencesRequest>()
            call.respondOk(repo.updatePreferences(call.currentUserId, body.channel))
        }
    }
}
