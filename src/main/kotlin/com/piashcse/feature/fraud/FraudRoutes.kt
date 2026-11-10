@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.fraud

import com.piashcse.model.response.PaymentResponse
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Admin velocity rules, blocklists and high-risk payment feed. */
fun Route.fraudAdminRoutes() {
    val fraudService: FraudService by inject()

    get("rules") {
        call.respondOk(fraudService.listRules())
    }.describe { summary = "List fraud velocity rules" }

    get("blocklist") {
        call.respondOk(fraudService.listBlocklist())
    }.describe { summary = "List blocklist entries" }

    get("high-risk") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(fraudService.listHighRisk(limit, offset))
    }.describe {
        summary = "List payments flagged for review or blocked"
        responses { ok<PaginatedResponse<PaymentResponse>>("High-risk payments") }
    }

    adminWriteRateLimit {
        post("rules") {
            call.respondCreated(fraudService.createRule(call.receive<FraudRuleRequest>()))
        }.describe {
            summary = "Create a fraud velocity rule"
            requestBody { jsonBody<FraudRuleRequest>() }
            responses { created<FraudRuleResponse>("Fraud rule created") }
        }

        put("rules/{id}") {
            call.respondOk(
                fraudService.updateRule(call.requirePathParameter("id"), call.receive<FraudRuleRequest>()),
            )
        }.describe {
            summary = "Update a fraud velocity rule"
            requestBody { jsonBody<FraudRuleRequest>() }
            responses { ok<FraudRuleResponse>("Fraud rule updated") }
        }

        delete("rules/{id}") {
            val id = call.requirePathParameter("id")
            if (!fraudService.deleteRule(id)) throw NotFoundException("Fraud rule not found")
            call.respondOk(MessageResponse("Fraud rule deleted"))
        }.describe { summary = "Delete a fraud velocity rule" }

        post("blocklist") {
            call.respondCreated(fraudService.createBlocklistEntry(call.receive<BlocklistRequest>()))
        }.describe {
            summary = "Add a blocklist entry"
            requestBody { jsonBody<BlocklistRequest>() }
            responses { created<BlocklistResponse>("Blocklist entry created") }
        }

        delete("blocklist/{id}") {
            val id = call.requirePathParameter("id")
            if (!fraudService.deleteBlocklistEntry(id)) throw NotFoundException("Blocklist entry not found")
            call.respondOk(MessageResponse("Blocklist entry deleted"))
        }.describe { summary = "Delete a blocklist entry" }
    }
}

/** Admin AVS/CVV result capture for a payment. */
fun Route.paymentRiskAdminRoutes() {
    val fraudService: FraudService by inject()
    adminWriteRateLimit {
        post("{id}/risk") {
            val id = call.requirePathParameter("id")
            fraudService.recordRiskResult(id, call.receive<RiskResultRequest>())
            call.respondOk(MessageResponse("Payment risk result recorded"))
        }.describe {
            summary = "Record AVS/CVV results for a payment"
            requestBody { jsonBody<RiskResultRequest>() }
            responses { ok<MessageResponse>("Risk result recorded") }
        }
    }
}

/** Admin chargeback/dispute workflow. */
fun Route.chargebackAdminRoutes() {
    val chargebackService: ChargebackService by inject()

    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(chargebackService.list(limit, offset))
    }.describe { summary = "List chargebacks" }

    get("{id}") {
        call.respondOk(chargebackService.get(call.requirePathParameter("id")))
    }.describe { summary = "Retrieve a chargeback" }

    adminWriteRateLimit {
        post {
            call.respondCreated(chargebackService.create(call.receive<ChargebackRequest>()))
        }.describe {
            summary = "Record a chargeback/dispute"
            requestBody { jsonBody<ChargebackRequest>() }
            responses { created<ChargebackResponse>("Chargeback recorded") }
        }

        put("{id}") {
            call.respondOk(
                chargebackService.update(call.requirePathParameter("id"), call.receive<ChargebackUpdateRequest>()),
            )
        }.describe {
            summary = "Update a chargeback status/outcome"
            requestBody { jsonBody<ChargebackUpdateRequest>() }
            responses { ok<ChargebackResponse>("Chargeback updated") }
        }
    }
}
