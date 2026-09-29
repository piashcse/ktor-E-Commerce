package com.piashcse

import com.piashcse.config.DotEnvConfig
import com.piashcse.database.closeDataSource
import com.piashcse.database.configureDatabase
import com.piashcse.event.EventBus
import com.piashcse.event.subscriber.AuditLogSubscriber
import com.piashcse.event.subscriber.EmailSubscriber
import com.piashcse.event.subscriber.NotificationSubscriber
import com.piashcse.feature.audit_log.AuditLogRepository
import com.piashcse.plugin.*
import com.piashcse.service.AsyncWorker
import com.piashcse.service.OutboxPoller
import com.piashcse.service.StockReservationCleanup
import com.piashcse.service.configureOutboxPoller
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.koin.ktor.ext.get

fun main() {
    val port = DotEnvConfig.serverPort
    val host = DotEnvConfig.serverHost
    // Graceful shutdown: Ktor stops accepting connections on SIGTERM and drains in-flight
    // requests (30s grace target). If the Netty engine factory in use exposes a
    // shutdownTimeout/shutdownGracePeriod setting, wire 30s there; otherwise this relies on
    // Engine.stop grace plus the ApplicationStopped hook below for resource cleanup.
    embeddedServer(Netty, port = port, host = host) {
        configureAll()
    }.start(wait = true)
}

fun Application.configureAll() {
    configureDatabase()
    configureBasic()
    configureMetrics()
    installRequestTracing()
    configureKoin()
    configureAuth()
    configureRateLimiting()
    configureSwagger()
    configureStatusPage()
    configureStaticContent()
    configureRoute()
    EventBus.subscribe(EmailSubscriber())
    EventBus.subscribe(AuditLogSubscriber(get<AuditLogRepository>()))
    EventBus.subscribe(NotificationSubscriber())
    EventBus.start(this)
    configureOutboxPoller()
    AsyncWorker.start(this)
    StockReservationCleanup.start(this)
    @Suppress("DEPRECATION")
    environment.monitor.subscribe(ApplicationStopped) {
        OutboxPoller.stop()
        closeDataSource()
        AsyncWorker.stop()
        StockReservationCleanup.stop()
        EventBus.stop()
    }
}
