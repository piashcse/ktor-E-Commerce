package com.piashcse.plugin

import com.piashcse.constants.Message
import com.piashcse.utils.common.ApiError
import com.piashcse.utils.common.FieldError
import com.piashcse.utils.validator.AppException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory
import org.valiktor.ConstraintViolationException
import org.valiktor.i18n.mapToMessage
import java.util.*

/**
 * Helper to find the first [ConstraintViolationException] in the cause chain.
 */
private fun Throwable.firstConstraintViolation(): ConstraintViolationException? =
    generateSequence(this) { it.cause }.filterIsInstance<ConstraintViolationException>().firstOrNull()

/**
 * Centralised response builder for validation errors.
 */
private suspend fun ApplicationCall.respondValidationError(exception: ConstraintViolationException) {
    val fieldErrors = exception.constraintViolations
        .mapToMessage(baseName = "messages", locale = Locale.ENGLISH)
        .map { FieldError(field = it.property, message = it.message) }
    respond(HttpStatusCode.BadRequest, ApiError(message = "Validation failed", errors = fieldErrors))
}

/**
 * Global exception handler — Industry‑standard (Stripe/GitHub/OpenAI).
 *
 * Success: Return data directly (HTTP status = source of truth)
 * Error: Return [ApiError] { message, errors? }
 *
 * All custom exceptions in the project extend [AppException] and are handled uniformly.
 */
private val statusPageLog = LoggerFactory.getLogger("com.piashcse.plugin.ConfigureStatusPage")

fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<Throwable> { call, error ->
            // 1️⃣ Validation errors (including wrapped ones)
            error.firstConstraintViolation()?.let {
                call.respondValidationError(it)
                return@exception
            }

            // 2️⃣ Application specific errors – all extend [AppException]
            when (error) {
                is AppException -> {
                    statusPageLog.warn("${error::class.simpleName}: ${error.message}")
                    call.respond(error.code, ApiError(error.message ?: Message.Errors.INTERNAL))
                }
                is BadRequestException -> {
                    call.respond(HttpStatusCode.BadRequest, ApiError(error.message ?: Message.Errors.VALIDATION_FAILED))
                }
                is MissingRequestParameterException -> {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Missing parameter: ${error.parameterName}"))
                }
                is NumberFormatException -> {
                    call.respond(HttpStatusCode.BadRequest, ApiError(Message.Validation.invalidFormat("number")))
                }
                is IllegalArgumentException -> {
                    call.respond(HttpStatusCode.BadRequest, ApiError(error.message ?: Message.Errors.VALIDATION_FAILED))
                }
                else -> {
                    statusPageLog.error("Unhandled exception: ${error::class.simpleName}", error)
                    call.respond(HttpStatusCode.InternalServerError, ApiError(Message.Errors.INTERNAL))
                }
            }
        }

        // 4️⃣ Standard HTTP status shortcuts
        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, ApiError(Message.Errors.UNAUTHORIZED))
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, ApiError(Message.Errors.NOT_FOUND))
        }
        status(HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.respond(HttpStatusCode.MethodNotAllowed, ApiError("Method not allowed"))
        }
    }
}
