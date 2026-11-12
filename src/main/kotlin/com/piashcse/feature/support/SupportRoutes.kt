@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.support

import com.piashcse.config.DotEnvConfig
import com.piashcse.constants.TicketPriority
import com.piashcse.constants.TicketStatus
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.standardErrors
import com.piashcse.service.UploadService
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.requirePathParameter
import io.ktor.server.response.header
import io.ktor.server.response.respondFile
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject
import java.io.File

/** Customer support tickets. */
fun Route.supportRoutes() {
    val service: SupportService by inject()
    requireRole {
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(service.myTickets(call.currentUserId, limit, offset))
        }.describe { summary = "List my support tickets" }

        get("{id}") {
            call.respondOk(service.myTicket(call.currentUserId, call.requirePathParameter("id")))
        }.describe { summary = "Get a support ticket and public replies" }

        post {
            call.respondCreated(service.create(call.currentUserId, call.receive<CreateTicketRequest>()))
        }.describe {
            summary = "Open a support ticket"
            requestBody { jsonBody<CreateTicketRequest>() }
            responses {
                created<TicketResponse>("Ticket created")
                standardErrors()
            }
        }

        post("{id}/messages") {
            call.respondOk(
                service.reply(call.currentUserId, call.requirePathParameter("id"), call.receive<TicketReplyRequest>()),
            )
        }.describe {
            summary = "Reply to a support ticket"
            requestBody { jsonBody<TicketReplyRequest>() }
            responses { ok<TicketResponse>("Reply added") }
        }

        post("{id}/attachments") {
            val ticketId = call.requirePathParameter("id")
            service.myTicket(call.currentUserId, ticketId)
            var attachment: TicketAttachmentResponse? = null
            call.receiveMultipart().forEachPart { part ->
                try {
                    if (part is PartData.FileItem) {
                        val bytes = UploadService.readAndValidateImagePart(part, "support attachment")
                        val storage = UploadService.uploadSupportAttachment(part, bytes)
                        attachment =
                            service.attach(
                                call.currentUserId,
                                ticketId,
                                part.originalFileName ?: "attachment",
                                part.contentType?.toString(),
                                storage,
                                bytes.size.toLong(),
                            )
                    }
                } finally {
                    part.dispose()
                }
            }
            call.respondCreated(attachment ?: throw ValidationException("A file is required"))
        }.describe { summary = "Attach an image to my support ticket" }

        get("{id}/attachments/{attachmentId}") {
            val download =
                service.attachmentForCustomer(
                    call.currentUserId,
                    call.requirePathParameter("id"),
                    call.requirePathParameter("attachmentId"),
                )
            respondPrivateAttachment(call, download)
        }.describe { summary = "Download my support attachment" }

        post("{id}/close") {
            call.respondOk(service.close(call.currentUserId, call.requirePathParameter("id")))
        }.describe { summary = "Close my support ticket" }

        post("{id}/satisfaction") {
            call.respondOk(
                service.rate(
                    call.currentUserId,
                    call.requirePathParameter("id"),
                    call.receive<TicketSatisfactionRequest>(),
                ),
            )
        }.describe {
            summary = "Rate a resolved/closed support ticket"
            requestBody { jsonBody<TicketSatisfactionRequest>() }
            responses { ok<TicketSummaryResponse>("Satisfaction recorded") }
        }
    }
}

/** Admin support queue. */
fun Route.supportAdminRoutes() {
    val service: SupportService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        val status = call.request.queryParameters["status"]?.parseEnum<TicketStatus>("ticket status")
        val priority = call.request.queryParameters["priority"]?.parseEnum<TicketPriority>("ticket priority")
        call.respondOk(
            service.adminTickets(
                status = status,
                priority = priority,
                assignedTo = call.request.queryParameters["assignedTo"],
                limit = limit,
                offset = offset,
            ),
        )
    }.describe { summary = "Admin: list support tickets" }

    get("{id}") {
        call.respondOk(service.adminTicket(call.requirePathParameter("id")))
    }.describe { summary = "Admin: get a ticket with internal notes" }

    adminWriteRateLimit {
        post("{id}/messages") {
            call.respondOk(
                service.adminReply(
                    call.currentUserId,
                    call.requirePathParameter("id"),
                    call.receive<TicketReplyRequest>(),
                ),
            )
        }.describe {
            summary = "Admin: reply or add an internal note"
            requestBody { jsonBody<TicketReplyRequest>() }
            responses { ok<TicketResponse>("Message added") }
        }

        put("{id}") {
            call.respondOk(
                service.adminUpdate(call.requirePathParameter("id"), call.receive<TicketUpdateRequest>(), call.currentUserId),
            )
        }.describe {
            summary = "Admin: update ticket status/priority/assignment"
            requestBody { jsonBody<TicketUpdateRequest>() }
            responses { ok<TicketSummaryResponse>("Ticket updated") }
        }
    }

    get("{id}/attachments/{attachmentId}") {
        val download =
            service.attachmentForAdmin(
                call.requirePathParameter("id"),
                call.requirePathParameter("attachmentId"),
            )
        respondPrivateAttachment(call, download)
    }.describe { summary = "Admin: download a support attachment" }
}

private suspend fun respondPrivateAttachment(
    call: io.ktor.server.application.ApplicationCall,
    download: com.piashcse.feature.support.SupportAttachmentDownload,
) {
    val root = File(DotEnvConfig.uploadDir, "support-attachments").canonicalFile
    val file = File(root, download.storageName).canonicalFile
    if (!file.canonicalPath.startsWith(root.canonicalPath) || !file.isFile) {
        throw NotFoundException("Attachment file not found")
    }
    call.response.header(HttpHeaders.CacheControl, "private, no-store")
    call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"${download.fileName}\"")
    download.contentType?.let { call.response.header(HttpHeaders.ContentType, it) }
    call.respondFile(file)
}
