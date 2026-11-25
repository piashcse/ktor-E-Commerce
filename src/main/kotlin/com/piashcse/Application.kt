package com.piashcse

import com.piashcse.config.AppConfig
import com.piashcse.config.DotEnvConfig
import com.piashcse.database.closeDataSource
import com.piashcse.database.configureDatabase
import com.piashcse.event.EventBus
import com.piashcse.event.subscriber.AuditLogSubscriber
import com.piashcse.event.subscriber.EmailSubscriber
import com.piashcse.event.subscriber.NotificationSubscriber
import com.piashcse.feature.audit_log.AuditLogRepository
import com.piashcse.feature.order.StockReservationCleanup
import com.piashcse.feature.subscription.SubscriptionBillingWorker
import com.piashcse.feature.subscription.SubscriptionService
import com.piashcse.plugin.*
import com.piashcse.service.AsyncWorker
import com.piashcse.service.MaintenanceCleanup
import com.piashcse.service.OutboxPoller
import com.piashcse.service.configureOutboxPoller
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.netty.*
import org.koin.ktor.ext.get

fun main() {
    AppConfig.validate()
    val port = DotEnvConfig.serverPort
    val host = DotEnvConfig.serverHost
    // Graceful shutdown: Ktor stops accepting connections on SIGTERM and drains in-flight
    // requests (30s grace target). If the Netty engine factory in use exposes a
    // shutdownTimeout/shutdownGracePeriod setting, wire 30s there; otherwise this relies on
    // Engine.stop grace plus the ApplicationStopped hook below for resource cleanup.
    val environment = applicationEnvironment { }
    embeddedServer(
        Netty,
        environment = environment,
        configure = {
            connectors =
                mutableListOf<io.ktor.server.engine.EngineConnectorConfig>(
                    EngineConnectorBuilder().apply {
                        this.host = host
                        this.port = port
                    },
                )
            shutdownGracePeriod = 30_000
            shutdownTimeout = 30_000
        },
    ) {
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
    install(AdminAuditPlugin)
    configureRateLimiting()
    configureSwagger()
    configureStatusPage()
    configureStaticContent()
    configureRoute()
    EventBus.subscribe(EmailSubscriber())
    EventBus.subscribe(AuditLogSubscriber(get<AuditLogRepository>()))
    EventBus.subscribe(get<NotificationSubscriber>())
    EventBus.start(this)
    configureOutboxPoller()
    AsyncWorker.start(this)
    StockReservationCleanup.start(this)
    MaintenanceCleanup.start(this)
    SubscriptionBillingWorker.start(this, get<SubscriptionService>())
    monitor.subscribe(ApplicationStopping) {
        // Stop producers and consumers before tearing down the datasource.
        OutboxPoller.stop()
        AsyncWorker.stop()
        StockReservationCleanup.stop()
        MaintenanceCleanup.stop()
        SubscriptionBillingWorker.stop()
        EventBus.stop()
    }
    monitor.subscribe(ApplicationStopped) {
        closeDataSource()
    }
}
