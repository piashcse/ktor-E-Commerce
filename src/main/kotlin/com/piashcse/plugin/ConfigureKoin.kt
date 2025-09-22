package com.piashcse.plugin

import com.piashcse.di.serviceModule
import io.ktor.server.application.*
import org.koin.core.logger.Level
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger(Level.INFO)
        modules(serviceModule)
    }
}