package com.piashcse

import com.piashcse.config.DotEnvConfig
import com.piashcse.database.configureDatabase
import com.piashcse.event.EventBus
import com.piashcse.event.subscriber.AuditLogSubscriber
import com.piashcse.event.subscriber.EmailSubscriber
import com.piashcse.plugin.*
import com.piashcse.service.AsyncWorker
import com.piashcse.service.StockReservationCleanup
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val port = DotEnvConfig.serverPort
    val host = DotEnvConfig.serverHost
    embeddedServer(Netty, port = port, host = host) {
        configureAll()
    }.start(wait = true)
}

fun Application.configureAll() {
    configureDatabase()
    configureBasic()
    installRequestTracing()
    configureKoin()
    configureAuth()
    configureRateLimiting()
    configureSwagger()
    configureStatusPage()
    configureStaticContent()
    configureRoute()
    EventBus.subscribe(EmailSubscriber())
    EventBus.subscribe(AuditLogSubscriber())
    EventBus.start(this)
    AsyncWorker.start(this)
    StockReservationCleanup.start(this)
    @Suppress("DEPRECATION")
    environment.monitor.subscribe(ApplicationStopped) {
        AsyncWorker.stop()
        StockReservationCleanup.stop()
        EventBus.stop()
    }
}
