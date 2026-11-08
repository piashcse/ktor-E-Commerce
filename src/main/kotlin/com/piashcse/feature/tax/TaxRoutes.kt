package com.piashcse.feature.tax

import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.docs
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import io.ktor.server.request.receive
import io.ktor.server.routing.*

/** Admin tax-rule management. */
fun Route.taxAdminRoutes() {
    get {
        call.respondOk(TaxService.list())
    }.docs("List tax rules") {
        responses { ok<List<TaxRuleResponse>>("Tax rules") }
    }

    adminWriteRateLimit {
        post {
            call.respondCreated(TaxService.upsert(call.receive<TaxRuleRequest>()))
        }.docs("Create or update tax rule", "Country/category rule; rate is a decimal fraction") {
            requestBody { jsonBody<TaxRuleRequest>() }
            responses { created<TaxRuleResponse>("Tax rule saved") }
        }

        put {
            call.respondOk(TaxService.upsert(call.receive<TaxRuleRequest>()))
        }.docs("Upsert tax rule") {
            requestBody { jsonBody<TaxRuleRequest>() }
            responses { ok<TaxRuleResponse>("Tax rule saved") }
        }

        delete("{id}") {
            val id =
                call.parameters["id"]?.takeIf { it.isNotBlank() }
                    ?: throw ValidationException("id is required")
            if (!TaxService.delete(id)) throw NotFoundException("Tax rule not found")
            call.respondOk(MessageResponse("Tax rule deleted"))
        }.docs("Delete tax rule")
    }
}
