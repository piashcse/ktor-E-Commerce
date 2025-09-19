package com.piashcse

import com.piashcse.config.DotEnvConfig
import com.piashcse.database.configureDataBase
import com.piashcse.feature.auth.JwtConfig
import com.piashcse.plugin.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val port = DotEnvConfig.serverPort
    val host = DotEnvConfig.serverHost
    embeddedServer(Netty, port = port, host = host) {
        JwtConfig.init()
        configureAll()
    }.start(wait = true)
}

fun Application.configureAll() {
    configureDataBase()
    configureBasic()
    configureKoin()
    configureRequestValidation()
    configureAuth()
    configureSwagger()
    configureStatusPage()
    configureRoute()
}
