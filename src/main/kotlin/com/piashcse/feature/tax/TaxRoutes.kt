package com.piashcse.feature.tax

import com.piashcse.plugin.adminWriteRateLimit
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
    }

    adminWriteRateLimit {
        post {
            call.respondCreated(TaxService.upsert(call.receive<TaxRuleRequest>()))
        }

        put {
            call.respondOk(TaxService.upsert(call.receive<TaxRuleRequest>()))
        }

        delete("{id}") {
            val id =
                call.parameters["id"]?.takeIf { it.isNotBlank() }
                    ?: throw ValidationException("id is required")
            if (!TaxService.delete(id)) throw NotFoundException("Tax rule not found")
            call.respondOk(MessageResponse("Tax rule deleted"))
        }
    }
}
