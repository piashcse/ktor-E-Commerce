package com.piashcse.feature.fx

import com.piashcse.database.entities.SupportedCurrencies
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.service.FxService
import com.piashcse.utils.common.ApiError
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class FxRateRequest(
    @Contextual val rate: BigDecimal,
)

@Serializable
data class FxRateResponse(
    val targetCurrency: String,
    val rate: String,
    val effectiveAt: String,
)

/** Admin FX-rate management so multi-currency checkout is operable without SQL. */
fun Route.fxAdminRoutes() {
    get {
        call.respondOk(FxService.listRates())
    }
    adminWriteRateLimit {
        put("{targetCurrency}") {
            val target = call.requirePathParameter("targetCurrency")
            if (target.uppercase() !in SupportedCurrencies.ALL) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("Unsupported currency: $target", code = "VALIDATION_FAILED"),
                )
                return@put
            }
            val body = call.receive<FxRateRequest>()
            if (body.rate <= BigDecimal.ZERO) throw ValidationException("FX rate must be positive")
            call.respondOk(FxService.upsertRate(target, body.rate))
        }
    }
}
