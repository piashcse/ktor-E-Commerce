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

private fun Throwable.firstConstraintViolation(): ConstraintViolationException? =
    generateSequence(this) { it.cause }.filterIsInstance<ConstraintViolationException>().firstOrNull()

private fun Throwable.isDuplicateKey(): Boolean =
    generateSequence(this) { it.cause }.any { t ->
        val msg = (t.message ?: "").lowercase()
        "duplicate" in msg || "unique" in msg || "23505" in msg ||
            t::class.simpleName?.contains("ConstraintViolation", ignoreCase = true) == true
    }

private suspend fun ApplicationCall.respondValidationError(exception: ConstraintViolationException) {
    val fieldErrors = exception.constraintViolations
        .mapToMessage(baseName = "messages", locale = Locale.ENGLISH)
        .map { FieldError(field = it.property, message = it.message) }
    respond(
        HttpStatusCode.BadRequest,
        ApiError(message = "Validation failed", code = "VALIDATION_FAILED", errors = fieldErrors, requestId = requestId()),
    )
}

private fun ApplicationCall.errorResponse(
    message: String,
    code: HttpStatusCode = HttpStatusCode.BadRequest,
    errorCode: String = "BAD_REQUEST",
) = ApiError(message = message, code = errorCode, requestId = requestId())

private val statusPageLog = LoggerFactory.getLogger("com.piashcse.plugin.ConfigureStatusPage")

fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<Throwable> { call, error ->
            error.firstConstraintViolation()?.let {
                call.respondValidationError(it)
                return@exception
            }

            // DB unique violations (wishlist/cart/review/idempotency races) → 409, not 500.
            if (error.isDuplicateKey()) {
                statusPageLog.warn("Duplicate key: ${error.message}")
                call.respond(HttpStatusCode.Conflict, call.errorResponse("Resource already exists", HttpStatusCode.Conflict, "CONFLICT"))
                return@exception
            }

            when (error) {
                is AppException -> {
                    statusPageLog.warn("${error::class.simpleName}: ${error.message}")
                    call.respond(error.code, call.errorResponse(error.message ?: Message.Errors.INTERNAL, error.code, error.errorCode))
                }
                // MissingRequestParameterException extends BadRequestException — must come first.
                is MissingRequestParameterException -> {
                    call.respond(HttpStatusCode.BadRequest, call.errorResponse("Missing parameter: ${error.parameterName}", HttpStatusCode.BadRequest, "MISSING_PARAMETER"))
                }
                is BadRequestException -> {
                    call.respond(HttpStatusCode.BadRequest, call.errorResponse(error.message ?: Message.Errors.VALIDATION_FAILED, HttpStatusCode.BadRequest, "VALIDATION_FAILED"))
                }
                is io.ktor.serialization.JsonConvertException,
                is kotlinx.serialization.SerializationException,
                -> {
                    call.respond(HttpStatusCode.BadRequest, call.errorResponse(Message.Errors.VALIDATION_FAILED, HttpStatusCode.BadRequest, "VALIDATION_FAILED"))
                }
                is NumberFormatException -> {
                    call.respond(HttpStatusCode.BadRequest, call.errorResponse(Message.Validation.invalidFormat("number"), HttpStatusCode.BadRequest, "INVALID_FORMAT"))
                }
                is IllegalArgumentException -> {
                    call.respond(HttpStatusCode.BadRequest, call.errorResponse(error.message ?: Message.Errors.VALIDATION_FAILED, HttpStatusCode.BadRequest, "VALIDATION_FAILED"))
                }
                else -> {
                    statusPageLog.error("Unhandled exception: ${error::class.simpleName}", error)
                    call.respond(HttpStatusCode.InternalServerError, call.errorResponse(Message.Errors.INTERNAL, HttpStatusCode.InternalServerError, "INTERNAL"))
                }
            }
        }

        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, call.errorResponse(Message.Errors.UNAUTHORIZED, HttpStatusCode.Unauthorized, "UNAUTHORIZED"))
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, call.errorResponse(Message.Errors.NOT_FOUND, HttpStatusCode.NotFound, "NOT_FOUND"))
        }
        status(HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.respond(HttpStatusCode.MethodNotAllowed, call.errorResponse("Method not allowed", HttpStatusCode.MethodNotAllowed, "METHOD_NOT_ALLOWED"))
        }
        status(HttpStatusCode.TooManyRequests) { call, _ ->
            call.respond(HttpStatusCode.TooManyRequests, call.errorResponse("Rate limit exceeded. Please try again later.", HttpStatusCode.TooManyRequests, "RATE_LIMITED"))
        }
    }
}
