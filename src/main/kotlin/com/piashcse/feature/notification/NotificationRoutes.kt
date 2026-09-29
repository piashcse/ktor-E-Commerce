package com.piashcse.feature.notification

import com.piashcse.plugin.customerAuth
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondOk
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class MarkAllReadResponse(val markedRead: Long)

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
}
