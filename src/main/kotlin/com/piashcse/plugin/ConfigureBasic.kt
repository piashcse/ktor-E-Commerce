package com.piashcse.plugin

import com.piashcse.config.DotEnvConfig
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.slf4j.event.Level
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }
}

fun Application.configureBasic() {
    configureCORS()
    configureContentNegotiation()
    configureCallLogging()
}

private fun Application.configureCORS() {
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
                    schemes = listOf(url.protocol.name),
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
            HttpMethod.Options,
        ).forEach { allowMethod(it) }
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-Requested-With")
    }
}

private fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            prettyPrint = true
            serializersModule = SerializersModule {
                contextual(LocalDateTimeSerializer)
                contextual(BigDecimalSerializer)
            }
        })
    }
}

private fun Application.configureCallLogging() {
    val httpStatusSuccess = 300
    val httpStatusRedirect = 400
    val sensitiveKeys = setOf("password", "token", "otp", "secret", "authorization")

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val path = call.request.path()
            val queryParams = call.request.queryParameters.entries()
                .joinToString(", ") { (key, values) ->
                    if (key.lowercase() in sensitiveKeys) "$key=[REDACTED]" else "$key=${values.joinToString()}"
                }
            val duration = call.processingTimeMillis()
            val remoteHost = call.request.origin.remoteHost
            val coloredStatus =
                when {
                    status == null -> "\u001B[33mUNKNOWN\u001B[0m"
                    status.value < httpStatusSuccess -> "\u001B[32m$status\u001B[0m"
                    status.value < httpStatusRedirect -> "\u001B[33m$status\u001B[0m"
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
