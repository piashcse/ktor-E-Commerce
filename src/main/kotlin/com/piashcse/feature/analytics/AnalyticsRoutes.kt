@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.analytics

import com.piashcse.plugin.generalRateLimit
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.utils.extension.respondOk
import io.ktor.http.HttpHeaders
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Public storefront event ingestion. */
fun Route.analyticsRoutes() {
    val service: AnalyticsService by inject()
    generalRateLimit {
        post("events") {
            val body = call.receive<AnalyticsBatchRequest>()
            val userAgent = call.request.headers[HttpHeaders.UserAgent]
            if (!AnalyticsIngestPolicy.shouldIngest(body.consent, userAgent)) {
                call.respondOk(AnalyticsIngestResponse(accepted = 0, rejected = body.events.size))
                return@post
            }
            call.respondOk(service.track(userId = null, events = body.events))
        }.describe {
            summary = "Ingest storefront analytics events"
            description = "Anonymous batch ingestion for page/product/cart/checkout funnel events"
            requestBody { jsonBody<AnalyticsBatchRequest>() }
            responses { ok<AnalyticsIngestResponse>("Events accepted") }
        }
    }
}

/** Admin funnel, attribution and product conversion reports. */
fun Route.analyticsAdminRoutes() {
    val service: AnalyticsService by inject()

    get("funnel") {
        call.respondOk(
            service.funnel(
                startDate = call.request.queryParameters["startDate"],
                endDate = call.request.queryParameters["endDate"],
            ),
        )
    }.describe { summary = "Conversion funnel report" }

    get("attribution") {
        call.respondOk(
            service.attribution(
                startDate = call.request.queryParameters["startDate"],
                endDate = call.request.queryParameters["endDate"],
            ),
        )
    }.describe { summary = "UTM/referrer attribution report" }

    get("products") {
        call.respondOk(
            service.productFunnel(
                startDate = call.request.queryParameters["startDate"],
                endDate = call.request.queryParameters["endDate"],
                limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20,
            ),
        )
    }.describe { summary = "Product view → cart → purchase conversion" }
}
