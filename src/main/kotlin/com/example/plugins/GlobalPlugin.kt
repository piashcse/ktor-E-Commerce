package com.example.plugins

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.schema.namer.DefaultSchemaNamer
import com.papsign.ktor.openapigen.schema.namer.SchemaNamer
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import kotlin.reflect.KType

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
    // Open api configuration
    install(OpenAPIGen) {
        // basic info
        info {
            version = "0.0.1"
            title = "Ktor Ecommerce"
            description = "Api Documentation for Ktor Ecommerce App"
            contact {
                name = "PLabs Corporation"
                email = "piash599@gmail.com"
            }
        }
        // describe the server, add as many as you want
        server("http://localhost:8080/") {
            description = "Ktor for local server"
        }
        //optional custom schema object namer
        replaceModule(DefaultSchemaNamer, object : SchemaNamer {
            val regex = Regex("[A-Za-z0-9_.]+")
            override fun get(type: KType): String {
                return type.toString().replace(regex) { it.value.split(".").last() }.replace(Regex(">|<|, "), "_")
            }
        })
    }
}