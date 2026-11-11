@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.support

import com.piashcse.constants.TicketPriority
import com.piashcse.constants.TicketStatus
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.standardErrors
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

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

        post("{id}/close") {
            call.respondOk(service.close(call.currentUserId, call.requirePathParameter("id")))
        }.describe { summary = "Close my support ticket" }
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
}
