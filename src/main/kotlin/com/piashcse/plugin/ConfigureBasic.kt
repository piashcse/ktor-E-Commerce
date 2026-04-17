package com.piashcse.plugin


import com.google.gson.JsonSerializer
import com.piashcse.config.DotEnvConfig
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import org.slf4j.event.Level
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun Application.configureBasic() {
    install(CORS) {
        val allowedOrigins = DotEnvConfig.allowedOrigins.split(",")
        
        // Allow all origins if "*" is in the list
        if (allowedOrigins.any { it.trim() == "*" }) {
            anyHost()
        } else {
            allowedOrigins.forEach { origin ->
                val trimmed = origin.trim()
                // Parse the URL to extract host and scheme
                val url = Url(trimmed)
                allowHost(
                    host = url.host,
                    schemes = listOf(url.protocol.name)
                )
            }
        }
        
        allowCredentials = true
        allowNonSimpleContentTypes = true
        listOf(
            HttpMethod.Put,
            HttpMethod.Post,
            HttpMethod.Delete,
            HttpMethod.Patch,
            HttpMethod.Options
        ).forEach { allowMethod(it) }
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-Requested-With")
        exposeHeader("X-Request-ID")
    }
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            registerTypeAdapter(
                LocalDateTime::class.java,
                JsonSerializer<LocalDateTime> { localDateTime, _, _ ->
                    com.google.gson.JsonPrimitive(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                }
            )
            // serializeNulls()
        }
    }
    install(CallLogging){
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val path = call.request.path()
            val queryParams =
                call.request.queryParameters
                    .entries()
                    .joinToString(", ") { "${it.key}=${it.value}" }
            val duration = call.processingTimeMillis()
            val remoteHost = call.request.origin.remoteHost
            val coloredStatus =
                when {
                    status == null -> "\u001B[33mUNKNOWN\u001B[0m"
                    status.value < 300 -> "\u001B[32m$status\u001B[0m"
                    status.value < 400 -> "\u001B[33m$status\u001B[0m"
                    else -> "\u001B[31m$status\u001B[0m"
                }
            val coloredMethod = "\u001B[36m$httpMethod\u001B[0m"
            """
            |
            |------------------------ Request Details ------------------------
            |Status: $coloredStatus
            |Method: $coloredMethod
            |Path: $path
            |Query Params: $queryParams
            |Remote Host: $remoteHost
            |User Agent: $userAgent
            |Duration: ${duration}ms
            |------------------------------------------------------------------
            |
      """.trimMargin()
        }
    }
}