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
 * Global exception handler — Industry-standard error responses.
 *
 * Error handling:
 *   AppException             → HTTP status from exception, message only
 *   ConstraintViolation      → 400 with structured field errors
 *   Ktor framework errors    → 400 Bad Request
 *   else                     → 500 Internal Server Error + log
 *
 * HTTP status code is the source of truth — no redundant codes in response body.
 */
fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<Throwable> { call, error ->
            when (error) {
                // ── 1. Invalid enum value ─────────────────────────────────
                is InvalidEnumValueException -> {
                    call.application.environment.log.warn("Invalid enum: ${error.invalidValue} for ${error.enumName}")
                    call.respond(error.code, ApiResponse.error(error.message ?: "Invalid value"))
                }

                // ── 2. Missing parameter ──────────────────────────────────
                is MissingParameterException -> {
                    call.respond(error.code, ApiResponse.badRequest(error.message ?: "Missing parameter"))
                }

                // ── 3. All application exceptions ─────────────────────────
                is AppException -> {
                    call.application.environment.log.warn("${error::class.simpleName}: ${error.message}")
                    call.respond(error.code, ApiResponse.fromException(error))
                }

                // ── 4. Validation failures (structured) ──────────────────
                is ConstraintViolationException -> {
                    val fieldErrors = error.constraintViolations
                        .mapToMessage(baseName = "messages", locale = Locale.ENGLISH)
                        .map { FieldError(field = it.property, message = it.message) }

                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.validationError(errors = fieldErrors)
                    )
                }

                // ── 5. Ktor framework exceptions ─────────────────────────
                is MissingRequestParameterException ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.badRequest("Missing parameter: ${error.parameterName}")
                    )

                is NumberFormatException ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.badRequest("Invalid numeric value")
                    )

                is IllegalArgumentException ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.badRequest(error.message ?: "Invalid argument")
                    )

                // ── 6. Catch-all (500 errors) ────────────────────────────
                else -> {
                    call.application.environment.log.error("Unhandled: ${error::class.simpleName}", error)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.internalError()
                    )
                }
            }
        }

        // JWT auth returns raw 401 — standardize
        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, ApiResponse.unauthorized())
        }

        // 404 Not Found
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, ApiResponse.notFound("Resource not found"))
        }

        // 405 Method Not Allowed
        status(HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.respond(HttpStatusCode.MethodNotAllowed, ApiResponse.error("Method not allowed"))
        }
    }
}
