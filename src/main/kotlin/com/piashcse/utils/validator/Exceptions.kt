package com.piashcse.utils.validator

import com.piashcse.constants.Message
import io.ktor.http.*

// ============================================================================
//  INDUSTRY-STANDARD EXCEPTION HIERARCHY
// ============================================================================

/** Base exception for all application errors. */
open class AppException(
    message: String,
    val code: HttpStatusCode = HttpStatusCode.BadRequest,
) : Exception(message)

// ─── 400 Bad Request ───────────────────────────────────────────────────────

class ValidationException(message: String) : AppException(message, HttpStatusCode.BadRequest)

class InvalidEnumValueException(
    message: String,
    val enumName: String,
    val invalidValue: String,
) : AppException(message, HttpStatusCode.BadRequest)

class MissingParameterException(parameterName: String) :
    AppException(Message.Errors.MISSING_PARAMETER.format(parameterName), HttpStatusCode.BadRequest)

// ─── 401 Unauthorized ──────────────────────────────────────────────────────

class UnauthorizedException(message: String = Message.Errors.UNAUTHORIZED) :
    AppException(message, HttpStatusCode.Unauthorized)

class InvalidCredentialsException(
    remainingAttempts: Int? = null,
) : AppException(
        buildMessage(remainingAttempts),
        HttpStatusCode.Unauthorized,
    ) {
    companion object {
        private fun buildMessage(remaining: Int?): String =
            if (remaining != null && remaining > 0) {
                "${Message.Auth.INVALID_CREDENTIALS}. $remaining attempts remaining."
            } else {
                Message.Auth.INVALID_CREDENTIALS
            }
    }
}

// ─── 403 Forbidden ─────────────────────────────────────────────────────────

class ForbiddenException(message: String = Message.Errors.FORBIDDEN) :
    AppException(message, HttpStatusCode.Forbidden)

// ─── 404 Not Found ─────────────────────────────────────────────────────────

class NotFoundException(message: String = Message.Errors.NOT_FOUND) :
    AppException(message, HttpStatusCode.NotFound)

// ─── 409 Conflict ──────────────────────────────────────────────────────────

class ConflictException(message: String) : AppException(message, HttpStatusCode.Conflict)
