@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.fx

import com.piashcse.database.entities.SupportedCurrencies
import com.piashcse.model.request.FxRateRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.jsonBody
import com.piashcse.service.FxService
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject
import java.math.BigDecimal

/** Public FX rates for storefront currency selection. */
fun Route.fxPublicRoutes() {
    val fxService: FxService by inject()
    get {
        call.respondOk(fxService.listRates())
    }.describe {
        summary = "List FX rates"
        description = "Public target-currency rates applied to checkout totals"
    }
}

/** Admin FX-rate management so multi-currency checkout is operable without SQL. */
fun Route.fxAdminRoutes() {
    val fxService: FxService by inject()
    get {
        call.respondOk(fxService.listRates())
    }.describe { summary = "List FX rates" }
    adminWriteRateLimit {
        put("{targetCurrency}") {
            val target = call.requirePathParameter("targetCurrency")
            if (target.uppercase() !in SupportedCurrencies.ALL) {
                throw ValidationException("Unsupported currency: $target")
            }
            val body = call.receive<FxRateRequest>()
            if (body.rate <= BigDecimal.ZERO) throw ValidationException("FX rate must be positive")
            call.respondOk(fxService.upsertRate(target, body.rate))
        }.describe {
            summary = "Set FX rate"
            requestBody { jsonBody<FxRateRequest>() }
        }
    }
}
