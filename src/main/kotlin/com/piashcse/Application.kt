package com.piashcse

import com.piashcse.dbhelper.DatabaseFactory
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.piashcse.plugins.*
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import org.slf4j.LoggerFactory

fun main() {
    //val environment = System.getenv("KTOR_ENVIRONMENT") ?: "development"
    val configName = "application.conf"
    val appEngineEnv = applicationEngineEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load(configName))
        log = LoggerFactory.getLogger("ktor.application")
        developmentMode = false
        module {
            DatabaseFactory.init()
            configureAuthentication()
            configureBasic()
            configureStatusPage()
            configureRouting2()
        }
        connector {
            host = config.property("ktor.deployment.host").getString()
            port = config.property("ktor.deployment.port").getString().toInt()
        }
    }
    embeddedServer(Netty, appEngineEnv).start(wait = true)
}
