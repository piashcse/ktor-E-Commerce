@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.shipping

import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.standardErrors
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Order-scoped shipment endpoints: quotes, label purchase and parcel history. */
fun Route.orderShipmentRoutes() {
    val service: ShipmentService by inject()
    requireRole {
        /**
         * Return live carrier rates for the order's destination and parcels.
         */
        post("{id}/shipping-quotes") {
            val body = call.receive<ShipmentQuoteRequest>()
            call.respondOk(service.quote(call.requirePathParameter("id"), call.currentUserId, body))
        }.describe {
            summary = "Get shipping rate quotes"
            requestBody { jsonBody<ShipmentQuoteRequest>() }
            responses {
                ok<List<CarrierRate>>("Carrier rates")
                standardErrors()
            }
        }

        /**
         * List shipments/parcels for an order.
         */
        get("{id}/shipments") {
            call.respondOk(service.list(call.requirePathParameter("id"), call.currentUserId))
        }.describe { summary = "List order shipments" }

        /**
         * Purchase a carrier label and allocate order lines to the parcel.
         */
        post("{id}/shipments") {
            val body = call.receive<PurchaseShipmentRequest>()
            call.respondCreated(service.purchaseLabel(call.requirePathParameter("id"), call.currentUserId, body))
        }.describe {
            summary = "Purchase a shipping label"
            description = "Omitting items ships every remaining unit; sellers/admins only"
            requestBody { jsonBody<PurchaseShipmentRequest>() }
            responses {
                ok<ShipmentResponse>("Shipment created")
                standardErrors()
            }
        }
    }
}

/** Shipment detail and carrier tracking webhooks. */
fun Route.shipmentRoutes() {
    val service: ShipmentService by inject()

    /**
     * Carrier tracking webhook (public, signature verified).
     */
    post("webhook/{provider}") {
        val provider = call.requirePathParameter("provider")
        val signature =
            call.request.headers["X-Carrier-Signature"]
                ?: call.request.headers["X-Webhook-Signature"]
        val result = service.handleWebhook(provider, call.receiveText(), signature)
        if (result.accepted) {
            call.respondOk(MessageResponse(result.message))
        } else {
            // 5xx tells the carrier to retry; 200 would permanently drop a
            // transient failure.
            call.respond(HttpStatusCode.InternalServerError, MessageResponse(result.message))
        }
    }.describe {
        summary = "Carrier tracking webhook"
        description = "Verifies the carrier signature and applies tracking status idempotently"
    }

    requireRole {
        /**
         * Retrieve one shipment by ID.
         */
        get("{id}") {
            call.respondOk(service.get(call.requirePathParameter("id"), call.currentUserId))
        }.describe { summary = "Retrieve a shipment" }
    }
}
