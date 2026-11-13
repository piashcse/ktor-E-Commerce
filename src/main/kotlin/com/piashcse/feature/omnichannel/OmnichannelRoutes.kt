@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.omnichannel

import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.http.ContentType
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Public store locator. */
fun Route.storeLocatorRoutes() {
    val service: OmnichannelService by inject()
    get {
        call.respondOk(
            service.stores(
                latitude = call.request.queryParameters["lat"]?.toDoubleOrNull(),
                longitude = call.request.queryParameters["lng"]?.toDoubleOrNull(),
                radiusKm = call.request.queryParameters["radiusKm"]?.toDoubleOrNull(),
                limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20,
            ),
        )
    }.describe {
        summary = "Find nearby pickup stores"
        description = "Active pickup locations, optionally sorted/filtered by lat/lng and radiusKm"
    }
}

/** Seller BOPIS workflow. */
fun Route.pickupSellerListRoutes() {
    val service: OmnichannelService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.pickupOrders(call.currentUserId, limit, offset))
    }.describe { summary = "Seller: list pickup orders" }
}

fun Route.pickupActionRoutes() {
    val service: OmnichannelService by inject()
    writeRateLimit {
        post("{id}/pickup/ready") {
            call.respondOk(service.markReady(call.currentUserId, call.requirePathParameter("id")))
        }.describe { summary = "Seller: mark a pickup order ready" }

        post("{id}/pickup/complete") {
            val id = call.requirePathParameter("id")
            call.respondOk(
                service.completePickup(
                    call.currentUserId,
                    id,
                    call.receive<PickupCompleteRequest>().pickupCode,
                ),
            )
        }.describe {
            summary = "Seller: verify pickup code and complete collection"
            requestBody { jsonBody<PickupCompleteRequest>() }
            responses { ok<com.piashcse.model.response.OrderResponse>("Pickup completed") }
        }
    }
}

/** Seller POS sales. */
fun Route.posSellerRoutes() {
    val service: OmnichannelService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.posSales(call.currentUserId, limit, offset))
    }.describe { summary = "Seller: list POS sales" }

    get("{id}/receipt") {
        call.respondText(
            service.posReceipt(call.currentUserId, call.requirePathParameter("id")),
            ContentType.Text.Html,
        )
    }.describe { summary = "Seller: printable POS receipt" }

    writeRateLimit {
        post {
            call.respondCreated(service.posSale(call.currentUserId, call.receive<PosSaleRequest>()))
        }.describe {
            summary = "Seller: create an in-store POS sale"
            requestBody { jsonBody<PosSaleRequest>() }
            responses { created<com.piashcse.model.response.OrderResponse>("POS sale created") }
        }
    }
}

/** Seller POS cash-drawer sessions. */
fun Route.posSessionSellerRoutes() {
    val service: OmnichannelService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.posSessions(call.currentUserId, limit, offset))
    }.describe { summary = "Seller: list POS cash-drawer sessions" }

    get("{id}/report") {
        call.respondText(
            service.posSessionReport(call.currentUserId, call.requirePathParameter("id")),
            ContentType.Text.Html,
        )
    }.describe { summary = "Seller: printable POS session report" }

    writeRateLimit {
        post("open") {
            call.respondCreated(service.openPosSession(call.currentUserId, call.receive<PosSessionOpenRequest>()))
        }.describe {
            summary = "Seller: open a POS cash-drawer session"
            requestBody { jsonBody<PosSessionOpenRequest>() }
            responses { created<PosSessionResponse>("POS session opened") }
        }

        post("{id}/close") {
            call.respondOk(
                service.closePosSession(
                    call.currentUserId,
                    call.requirePathParameter("id"),
                    call.receive<PosSessionCloseRequest>(),
                ),
            )
        }.describe {
            summary = "Seller: close a POS session and reconcile cash"
            requestBody { jsonBody<PosSessionCloseRequest>() }
            responses { ok<PosSessionResponse>("POS session closed") }
        }
    }
}
