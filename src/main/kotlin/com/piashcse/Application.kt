package com.piashcse

import com.piashcse.config.DotEnvConfig
import com.piashcse.database.configureDatabase
import com.piashcse.plugin.*
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
    configureKoin()
    configureAuth()
    configureRateLimiting()
    configureSwagger()
    configureStatusPage()
    configureStaticContent()
    configureRoute()
}
