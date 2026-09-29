package com.piashcse.plugin

import com.piashcse.config.DotEnvConfig
import com.piashcse.utils.common.ApiError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun Application.configureMetrics() {
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
        meterBinders =
            listOf(
                ClassLoaderMetrics(),
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                ProcessorMetrics(),
                JvmThreadMetrics(),
                UptimeMetrics(),
            )
    }
    routing {
        get("/metrics") {
            // Internal endpoint: token auth when METRICS_TOKEN is set, loopback-only otherwise.
            val expected = DotEnvConfig.metricsToken
            val provided = call.request.headers["X-Metrics-Token"]
            val loopback = call.request.local.remoteHost in listOf("localhost", "127.0.0.1", "0:0:0:0:0:0:0:1", "::1")
            val authorized = (expected.isNotBlank() && provided == expected) || (expected.isBlank() && loopback)
            if (!authorized) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiError(message = "Forbidden", code = "FORBIDDEN", requestId = runCatching { call.requestId() }.getOrNull()),
                )
                return@get
            }
            call.respondText(appMicrometerRegistry.scrape())
        }
    }
}
