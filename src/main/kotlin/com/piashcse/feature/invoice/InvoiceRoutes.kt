@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.invoice

import com.piashcse.constants.UserType
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.requireUserType
import io.ktor.http.ContentType
import io.ktor.server.request.requirePathParameter
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Printable HTML documents for orders; clients can print or convert to PDF. */
fun Route.orderDocumentRoutes() {
    val service: InvoiceService by inject()
    requireRole(UserType.CUSTOMER, UserType.SELLER, UserType.ADMIN, UserType.SUPER_ADMIN) {
        get("{id}/invoice") {
            val html =
                service.invoice(
                    call.requirePathParameter("id"),
                    call.currentUserId,
                    call.requireUserType(),
                )
            call.respondText(html, ContentType.Text.Html)
        }.describe {
            summary = "Printable order invoice"
            description = "HTML invoice for the order owner, shop seller or admin"
        }

        get("{id}/packing-slip") {
            val html =
                service.packingSlip(
                    call.requirePathParameter("id"),
                    call.currentUserId,
                    call.requireUserType(),
                )
            call.respondText(html, ContentType.Text.Html)
        }.describe {
            summary = "Printable packing slip"
            description = "HTML packing slip without prices"
        }
    }
}
