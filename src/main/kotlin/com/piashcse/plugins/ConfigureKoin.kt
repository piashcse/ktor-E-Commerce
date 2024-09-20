package com.piashcse.plugins

import com.piashcse.di.controllerModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.core.logger.Level

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger(Level.INFO)
        modules(controllerModule)
    }
}