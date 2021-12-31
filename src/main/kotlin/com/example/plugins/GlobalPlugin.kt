package com.example.plugins

import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*

fun Application.configureBasic() {
    install(Compression)
    install(CORS) {
        anyHost()
    }
    install(CallLogging)
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            // serializeNulls()
        }
    }
}