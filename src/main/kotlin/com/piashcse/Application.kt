package com.piashcse

import com.piashcse.database.configureDataBase
import com.piashcse.plugins.*
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val configName = "application.conf"
    val appEngineEnv = applicationEngineEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load(configName))
        module {
            configureDataBase()
            configureBasic()
            configureKoin()
            configureRequestValidation()
            configureAuth()
            configureSwagger()
            configureStatusPage()
            configureRoute()
        }
        connector {
            host = config.property("ktor.deployment.host").getString()
            port = config.property("ktor.deployment.port").getString().toInt()
        }
    }
    embeddedServer(Netty, appEngineEnv).start(wait = true)
}
