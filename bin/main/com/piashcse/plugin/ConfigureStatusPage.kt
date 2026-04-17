package com.piashcse.plugin

import com.piashcse.utils.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.valiktor.ConstraintViolationException
import org.valiktor.i18n.mapToMessage
import java.util.*

/**
 * Global exception handler — Industry-standard (Stripe/GitHub/OpenAI).
 *
 * Success: Return data directly (HTTP status = source of truth)
 * Error: Return ApiError { message, errors? }
 */
fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<Throwable> { call, error ->
            when (error) {
                is InvalidEnumValueException -> {
                    call.application.environment.log.warn("Invalid enum: ${error.invalidValue} for ${error.enumName}")
                    call.respond(error.code, ApiError(error.message ?: "Invalid value"))
                }

                is MissingParameterException -> {
                    call.respond(error.code, ApiError(error.message ?: "Missing parameter"))
                }

                is AppException -> {
                    call.application.environment.log.warn("${error::class.simpleName}: ${error.message}")
                    call.respond(error.code, ApiError(error.message ?: "Unknown error"))
                }

                is ConstraintViolationException -> {
                    val fieldErrors = error.constraintViolations
                        .mapToMessage(baseName = "messages", locale = Locale.ENGLISH)
                        .map { FieldError(field = it.property, message = it.message) }

                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError(message = "Validation failed", errors = fieldErrors)
                    )
                }

                is MissingRequestParameterException ->
                    call.respond(HttpStatusCode.BadRequest, ApiError("Missing parameter: ${error.parameterName}"))

                is NumberFormatException ->
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid numeric value"))

                is IllegalArgumentException ->
                    call.respond(HttpStatusCode.BadRequest, ApiError(error.message ?: "Invalid argument"))

                else -> {
                    call.application.environment.log.error("Unhandled: ${error::class.simpleName}", error)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Internal server error"))
                }
            }
        }

        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, ApiError("Authentication required"))
        }

        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, ApiError("Resource not found"))
        }

        status(HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.respond(HttpStatusCode.MethodNotAllowed, ApiError("Method not allowed"))
        }
    }
}
