package com.piashcse.plugin

import com.piashcse.constants.Message
import com.piashcse.utils.common.ApiError
import com.piashcse.utils.common.FieldError
import com.piashcse.utils.db.isCheckViolation
import com.piashcse.utils.db.isDataException
import com.piashcse.utils.db.isForeignKeyViolation
import com.piashcse.utils.db.isNotNullViolation
import com.piashcse.utils.db.isUniqueViolation
import com.piashcse.utils.validator.AppException
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.InvalidEnumValueException
import com.piashcse.utils.validator.MissingParameterException
import io.ktor.http.*
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.SerializationException
import org.slf4j.LoggerFactory
import org.valiktor.ConstraintViolationException
import org.valiktor.i18n.mapToMessage
import java.util.*

private fun Throwable.firstConstraintViolation(): ConstraintViolationException? =
    generateSequence(this) { it.cause }.filterIsInstance<ConstraintViolationException>().firstOrNull()

private fun Throwable.firstAppException(): AppException? =
    generateSequence(this) { it.cause }.filterIsInstance<AppException>().firstOrNull()

private suspend fun ApplicationCall.respondAppException(error: AppException) {
    when (error) {
        is FieldValidationException -> {
            statusPageLog.warn("Validation failed: ${error.violations.joinToString { it.field }}")
            respond(
                HttpStatusCode.BadRequest,
                errorResponse(error.message ?: "Validation failed", HttpStatusCode.BadRequest, error.errorCode)
                    .copy(errors = error.violations.map { FieldError(it.field, it.message) }),
            )
        }
        is MissingParameterException -> {
            respond(
                HttpStatusCode.BadRequest,
                errorResponse(error.message ?: "Missing parameter", HttpStatusCode.BadRequest, error.errorCode)
                    .copy(errors = listOf(FieldError(error.parameterName, error.message ?: "Missing parameter"))),
            )
        }
        is InvalidEnumValueException -> {
            respond(
                HttpStatusCode.BadRequest,
                errorResponse(error.message ?: "Invalid value", HttpStatusCode.BadRequest, error.errorCode)
                    .copy(errors = error.fieldName?.let { listOf(FieldError(it, error.message ?: "Invalid value")) }),
            )
        }
        else -> {
            statusPageLog.warn("${error::class.simpleName}: ${error.message}")
            respond(error.code, errorResponse(error.message ?: Message.Errors.INTERNAL, error.code, error.errorCode))
        }
    }
}

private suspend fun ApplicationCall.respondValidationError(exception: ConstraintViolationException) {
    val fieldErrors =
        exception.constraintViolations
            .mapToMessage(baseName = "messages", locale = Locale.ENGLISH)
            .map { FieldError(field = it.property, message = it.message) }
    respond(
        HttpStatusCode.BadRequest,
        ApiError(
            message = "Validation failed",
            code = "VALIDATION_FAILED",
            errors = fieldErrors,
            requestId = requestId(),
            type = "VALIDATION_FAILED",
            title = "Validation failed",
            status = HttpStatusCode.BadRequest.value,
        ),
    )
}

private fun ApplicationCall.errorResponse(
    message: String,
    code: HttpStatusCode = HttpStatusCode.BadRequest,
    errorCode: String = "BAD_REQUEST",
) = ApiError(
    message = message,
    code = errorCode,
    requestId = requestId(),
    // Additive RFC 9457-style aliases; message/code remain the canonical fields.
    type = errorCode,
    title = message,
    status = code.value,
)

private val statusPageLog = LoggerFactory.getLogger("com.piashcse.plugin.ConfigureStatusPage")

fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<Throwable> { call, error ->
            error.firstConstraintViolation()?.let {
                call.respondValidationError(it)
                return@exception
            }

            // DB constraint violations → deterministic client errors, not 500s.
            if (error.isUniqueViolation()) {
                statusPageLog.warn("Duplicate key: ${error.message}")
                call.respond(HttpStatusCode.Conflict, call.errorResponse("Resource already exists", HttpStatusCode.Conflict, "CONFLICT"))
                return@exception
            }

            if (error.isForeignKeyViolation()) {
                statusPageLog.warn("Foreign-key violation: ${error.message}")
                call.respond(
                    HttpStatusCode.Conflict,
                    call.errorResponse("Resource is still referenced by other records", HttpStatusCode.Conflict, "CONFLICT"),
                )
                return@exception
            }

            if (error.isNotNullViolation() || error.isCheckViolation() || error.isDataException()) {
                statusPageLog.warn("Constraint violation: ${error.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    call.errorResponse(Message.Errors.VALIDATION_FAILED, HttpStatusCode.BadRequest, "VALIDATION_FAILED"),
                )
                return@exception
            }

            // DTO validation can be wrapped by Ktor's body-conversion exception; unwrap it
            // so field-level errors survive.
            error.firstAppException()?.let { appError ->
                call.respondAppException(appError)
                return@exception
            }

            when (error) {
                // MissingRequestParameterException extends BadRequestException — must come first.
                is MissingRequestParameterException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        call.errorResponse("Missing parameter: ${error.parameterName}", HttpStatusCode.BadRequest, "MISSING_PARAMETER")
                            .copy(errors = listOf(FieldError(error.parameterName, "Missing required parameter"))),
                    )
                }
                is BadRequestException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        call.errorResponse(
                            error.message ?: Message.Errors.VALIDATION_FAILED,
                            HttpStatusCode.BadRequest,
                            "VALIDATION_FAILED",
                        ),
                    )
                }
                is JsonConvertException,
                is SerializationException,
                -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        call.errorResponse(Message.Errors.VALIDATION_FAILED, HttpStatusCode.BadRequest, "VALIDATION_FAILED"),
                    )
                }
                is NumberFormatException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        call.errorResponse(Message.Validation.invalidFormat("number"), HttpStatusCode.BadRequest, "INVALID_FORMAT"),
                    )
                }
                is IllegalArgumentException -> {
                    // `require(...)` leftovers are programming/config errors, not client input.
                    statusPageLog.error("Unmapped IllegalArgumentException", error)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        call.errorResponse(Message.Errors.INTERNAL, HttpStatusCode.InternalServerError, "INTERNAL"),
                    )
                }
                else -> {
                    statusPageLog.error("Unhandled exception: ${error::class.simpleName}", error)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        call.errorResponse(Message.Errors.INTERNAL, HttpStatusCode.InternalServerError, "INTERNAL"),
                    )
                }
            }
        }

        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respond(
                HttpStatusCode.Unauthorized,
                call.errorResponse(Message.Errors.UNAUTHORIZED, HttpStatusCode.Unauthorized, "UNAUTHORIZED"),
            )
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, call.errorResponse(Message.Errors.NOT_FOUND, HttpStatusCode.NotFound, "NOT_FOUND"))
        }
        status(HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.respond(
                HttpStatusCode.MethodNotAllowed,
                call.errorResponse("Method not allowed", HttpStatusCode.MethodNotAllowed, "METHOD_NOT_ALLOWED"),
            )
        }
        status(HttpStatusCode.TooManyRequests) { call, _ ->
            call.respond(
                HttpStatusCode.TooManyRequests,
                call.errorResponse("Rate limit exceeded. Please try again later.", HttpStatusCode.TooManyRequests, "RATE_LIMITED"),
            )
        }
        status(HttpStatusCode.PayloadTooLarge) { call, _ ->
            call.respond(
                HttpStatusCode.PayloadTooLarge,
                call.errorResponse("Request payload is too large", HttpStatusCode.PayloadTooLarge, "PAYLOAD_TOO_LARGE"),
            )
        }
        status(HttpStatusCode.UnsupportedMediaType) { call, _ ->
            call.respond(
                HttpStatusCode.UnsupportedMediaType,
                call.errorResponse("Unsupported media type", HttpStatusCode.UnsupportedMediaType, "UNSUPPORTED_MEDIA_TYPE"),
            )
        }
    }
}
