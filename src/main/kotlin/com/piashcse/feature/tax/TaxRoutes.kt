@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.tax

import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Admin tax-rule management. */
fun Route.taxAdminRoutes() {
    val taxService: TaxService by inject()
    get {
        call.respondOk(taxService.list())
    }.describe {
        summary = "List tax rules"
        responses { ok<List<TaxRuleResponse>>("Tax rules") }
    }

    adminWriteRateLimit {
        post {
            call.respondCreated(taxService.upsert(call.receive<TaxRuleRequest>()))
        }.describe {
            summary = "Create or update tax rule"
            description = "Country/zone/class rule; rate is a decimal fraction"
            requestBody { jsonBody<TaxRuleRequest>() }
            responses { created<TaxRuleResponse>("Tax rule saved") }
        }

        put {
            call.respondOk(taxService.upsert(call.receive<TaxRuleRequest>()))
        }.describe {
            summary = "Upsert tax rule"
            requestBody { jsonBody<TaxRuleRequest>() }
            responses { ok<TaxRuleResponse>("Tax rule saved") }
        }

        delete("{id}") {
            val id = call.requirePathParameter("id")
            if (!taxService.delete(id)) throw NotFoundException("Tax rule not found")
            call.respondOk(MessageResponse("Tax rule deleted"))
        }.describe { summary = "Delete tax rule" }
    }
}

/** Admin tax-class management (standard, reduced, zero-rated, ...). */
fun Route.taxClassAdminRoutes() {
    val taxService: TaxService by inject()
    get {
        call.respondOk(taxService.listClasses())
    }.describe { summary = "List tax classes" }

    adminWriteRateLimit {
        post {
            call.respondCreated(taxService.createClass(call.receive<TaxClassRequest>()))
        }.describe {
            summary = "Create a tax class"
            requestBody { jsonBody<TaxClassRequest>() }
            responses { created<TaxClassResponse>("Tax class created") }
        }

        put("{id}") {
            call.respondOk(
                taxService.updateClass(call.requirePathParameter("id"), call.receive<TaxClassRequest>()),
            )
        }.describe {
            summary = "Update a tax class"
            requestBody { jsonBody<TaxClassRequest>() }
            responses { ok<TaxClassResponse>("Tax class updated") }
        }

        delete("{id}") {
            val id = call.requirePathParameter("id")
            if (!taxService.deleteClass(id)) throw NotFoundException("Tax class not found")
            call.respondOk(MessageResponse("Tax class deleted"))
        }.describe { summary = "Delete a tax class" }
    }
}

/** Admin tax-zone/nexus management. */
fun Route.taxZoneAdminRoutes() {
    val taxService: TaxService by inject()
    get {
        call.respondOk(taxService.listZones())
    }.describe { summary = "List tax zones" }

    adminWriteRateLimit {
        post {
            call.respondCreated(taxService.createZone(call.receive<TaxZoneRequest>()))
        }.describe {
            summary = "Create a tax zone"
            requestBody { jsonBody<TaxZoneRequest>() }
            responses { created<TaxZoneResponse>("Tax zone created") }
        }

        put("{id}") {
            call.respondOk(
                taxService.updateZone(call.requirePathParameter("id"), call.receive<TaxZoneRequest>()),
            )
        }.describe {
            summary = "Update a tax zone"
            requestBody { jsonBody<TaxZoneRequest>() }
            responses { ok<TaxZoneResponse>("Tax zone updated") }
        }

        delete("{id}") {
            val id = call.requirePathParameter("id")
            if (!taxService.deleteZone(id)) throw NotFoundException("Tax zone not found")
            call.respondOk(MessageResponse("Tax zone deleted"))
        }.describe { summary = "Delete a tax zone" }
    }
}

/** Admin customer/business tax-exemption certificates. */
fun Route.taxExemptionAdminRoutes() {
    val taxService: TaxService by inject()
    get {
        call.respondOk(taxService.listExemptions())
    }.describe { summary = "List tax exemptions" }

    adminWriteRateLimit {
        post {
            call.respondCreated(taxService.createExemption(call.receive<TaxExemptionRequest>()))
        }.describe {
            summary = "Create a tax exemption"
            requestBody { jsonBody<TaxExemptionRequest>() }
            responses { created<TaxExemptionResponse>("Tax exemption created") }
        }

        delete("{id}") {
            val id = call.requirePathParameter("id")
            if (!taxService.deleteExemption(id)) throw NotFoundException("Tax exemption not found")
            call.respondOk(MessageResponse("Tax exemption deleted"))
        }.describe { summary = "Delete a tax exemption" }
    }
}

/** Admin product-to-tax-class assignment. */
fun Route.productTaxClassAdminRoutes() {
    val taxService: TaxService by inject()
    adminWriteRateLimit {
        put("{id}/tax-class") {
            val productId = call.requirePathParameter("id")
            val body = call.receive<ProductTaxClassRequest>()
            taxService.assignProductTaxClass(productId, body.taxClassId)
            call.respondOk(MessageResponse("Product tax class updated"))
        }.describe {
            summary = "Assign a product tax class"
            requestBody { jsonBody<ProductTaxClassRequest>() }
            responses { ok<MessageResponse>("Product tax class updated") }
        }
    }
}
