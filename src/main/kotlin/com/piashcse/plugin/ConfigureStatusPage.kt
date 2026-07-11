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

private suspend fun ApplicationCall.respondValidationError(exception: ConstraintViolationException) {
    val fieldErrors = exception.constraintViolations
        .mapToMessage(baseName = "messages", locale = Locale.ENGLISH)
        .map { FieldError(field = it.property, message = it.message) }
    respond(
        HttpStatusCode.BadRequest,
        ApiError(message = "Validation failed", errors = fieldErrors, requestId = requestId()),
    )
}

private fun ApplicationCall.errorResponse(
    message: String,
    code: HttpStatusCode = HttpStatusCode.BadRequest,
) = ApiError(message = message, requestId = requestId())

private val statusPageLog = LoggerFactory.getLogger("com.piashcse.plugin.ConfigureStatusPage")

fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<Throwable> { call, error ->
            error.firstConstraintViolation()?.let {
                call.respondValidationError(it)
                return@exception
            }

            when (error) {
                is AppException -> {
                    statusPageLog.warn("${error::class.simpleName}: ${error.message}")
                    call.respond(error.code, call.errorResponse(error.message ?: Message.Errors.INTERNAL))
                }
                is BadRequestException -> {
                    call.respond(HttpStatusCode.BadRequest, call.errorResponse(error.message ?: Message.Errors.VALIDATION_FAILED))
                }
                is MissingRequestParameterException -> {
                    call.respond(HttpStatusCode.BadRequest, call.errorResponse("Missing parameter: ${error.parameterName}"))
                }
                is NumberFormatException -> {
                    call.respond(HttpStatusCode.BadRequest, call.errorResponse(Message.Validation.invalidFormat("number")))
                }
                is IllegalArgumentException -> {
                    call.respond(HttpStatusCode.BadRequest, call.errorResponse(error.message ?: Message.Errors.VALIDATION_FAILED))
                }
                else -> {
                    statusPageLog.error("Unhandled exception: ${error::class.simpleName}", error)
                    call.respond(HttpStatusCode.InternalServerError, call.errorResponse(Message.Errors.INTERNAL))
                }
            }
        }

        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, call.errorResponse(Message.Errors.UNAUTHORIZED))
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, call.errorResponse(Message.Errors.NOT_FOUND))
        }
        status(HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.respond(HttpStatusCode.MethodNotAllowed, call.errorResponse("Method not allowed"))
        }
    }
}
