@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.notification

import com.piashcse.constants.Permission
import com.piashcse.plugin.created
import com.piashcse.plugin.customerAuth
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.requirePermission
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.cursorRequest
import com.piashcse.utils.extension.getCurrentUserType
import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.queryBoolean
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

fun Route.notificationRoutes() {
    val repo: NotificationRepository by inject()
    val prefService: NotificationPreferencesService by inject()
    customerAuth {
        get {
            val (limit, offset) = call.pageRequest()
            val unreadOnly = call.queryBoolean("unreadOnly")
            call.respondOk(repo.list(call.currentUserId, limit, offset, unreadOnly))
        }.describe {
            summary = "List notifications"
            description = "Preference rows are hidden from the inbox"
        }
        get("unread-count") {
            call.respondOk(UnreadCountResponse(repo.unreadCount(call.currentUserId)))
        }.describe {
            summary = "Unread notification count"
            responses { ok<UnreadCountResponse>("Unread count") }
        }
        get("cursor") {
            val cursor = call.cursorRequest()
            val unreadOnly = call.queryBoolean("unreadOnly")
            call.respondOk(
                repo.listCursor(
                    call.currentUserId,
                    cursor.limit,
                    cursor.cursor,
                    unreadOnly,
                ),
            )
        }.describe { summary = "Cursor-paginated notifications" }
        post("{id}/read") {
            call.respondOk(repo.markRead(call.currentUserId, call.requirePathParameter("id")))
        }.describe {
            summary = "Mark notification read"
            responses { ok<NotificationResponse>("Notification") }
        }
        post("read-all") {
            call.respondOk(MarkAllReadResponse(repo.markAllRead(call.currentUserId)))
        }.describe {
            summary = "Mark all notifications read"
            responses { ok<MarkAllReadResponse>("Number marked read") }
        }
    }
    requirePermission(Permission.NOTIFICATION_SEND) {
        writeRateLimit {
            post("send") {
                val body = call.receive<SendNotificationRequest>()
                call.respondCreated(
                    repo.send(
                        actorUserId = call.currentUserId,
                        actorType = call.getCurrentUserType(),
                        userId = body.userId,
                        title = body.title,
                        body = body.body,
                        channel = body.channel,
                        type = body.type,
                    ),
                )
            }.describe {
                summary = "Send notification"
                description = "Admins may target anyone; sellers only their own customers"
                requestBody { jsonBody<SendNotificationRequest>() }
                responses { created<NotificationResponse>("Notification sent") }
            }
        }
    }
    requireRole {
        get("preferences") {
            call.respondOk(repo.getPreferences(call.currentUserId))
        }.describe {
            summary = "Get global notification channel"
            responses { ok<NotificationPreferencesResponse>("Current default channel") }
        }
        put("preferences") {
            val body = call.receive<UpdateNotificationPreferencesRequest>()
            call.respondOk(repo.updatePreferences(call.currentUserId, body.channel))
        }.describe {
            summary = "Set global notification channel"
            requestBody { jsonBody<UpdateNotificationPreferencesRequest>() }
            responses { ok<NotificationPreferencesResponse>("Updated channel") }
        }
        get("preferences/{eventType}") {
            val eventType = call.requirePathParameter("eventType")
            call.respondOk(prefService.get(call.currentUserId, eventType))
        }.describe {
            summary = "Get per-event channel"
            description = "Falls back to the global default"
            responses { ok<NotificationPreferenceResponse>("Resolved channel") }
        }
        put("preferences/{eventType}") {
            val eventType = call.requirePathParameter("eventType")
            val body = call.receive<UpdateNotificationPreferencesRequest>()
            call.respondOk(prefService.set(call.currentUserId, eventType, body.channel))
        }.describe {
            summary = "Set per-event channel"
            description = "In-app rows are always written; this controls the extra channel"
            requestBody { jsonBody<UpdateNotificationPreferencesRequest>() }
            responses { ok<NotificationPreferenceResponse>("Updated preference") }
        }
    }
}

/** Seller-consolidated alias: `/seller/notifications/send`. */
fun Route.sellerNotificationRoutes() {
    val repo: NotificationRepository by inject()
    writeRateLimit {
        post("send") {
            val body = call.receive<SendNotificationRequest>()
            call.respondCreated(
                repo.send(
                    actorUserId = call.currentUserId,
                    actorType = call.getCurrentUserType(),
                    userId = body.userId,
                    title = body.title,
                    body = body.body,
                    channel = body.channel,
                    type = body.type,
                ),
            )
        }.describe {
            summary = "Seller: notify one of my customers"
            requestBody { jsonBody<SendNotificationRequest>() }
        }
    }
}
