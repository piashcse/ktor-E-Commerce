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
            description = "Country/category rule; rate is a decimal fraction"
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
