package com.piashcse

import com.piashcse.database.configureDataBase
import com.piashcse.plugins.*
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    val port = config.property("ktor.deployment.port").getString().toInt()
    val host = config.property("ktor.deployment.host").getString()
    embeddedServer(Netty, port = port, host = host) {
        configureDataBase()
        configureBasic()
        configureKoin()
        configureRequestValidation()
        configureAuth()
        configureSwagger()
        configureStatusPage()
        configureRoute()
    }.start(wait = true)
}
