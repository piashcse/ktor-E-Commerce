package com.piashcse.plugin

import com.piashcse.di.authModule
import com.piashcse.di.featureServiceModule
import com.piashcse.di.gatewayModule
import com.piashcse.di.repositoryModule
import io.ktor.server.application.*
import org.koin.core.logger.Level
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger(Level.INFO)
        modules(repositoryModule, featureServiceModule, gatewayModule, authModule)
    }
}
