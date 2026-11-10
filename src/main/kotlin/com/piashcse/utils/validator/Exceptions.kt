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
    /** Machine-readable error code (Stripe-style `code`). */
    open val errorCode: String = "BAD_REQUEST",
) : Exception(message)

/** Field-level validation failure that maps directly to `ApiError.errors`. */
data class FieldViolation(
    val field: String,
    val message: String,
)

class FieldValidationException(
    val violations: List<FieldViolation>,
    message: String = "Validation failed",
) : AppException(message, HttpStatusCode.BadRequest, "VALIDATION_FAILED")

// ─── 400 Bad Request ───────────────────────────────────────────────────────

class ValidationException(message: String, override val errorCode: String = "VALIDATION_FAILED") : AppException(
    message,
    HttpStatusCode.BadRequest,
    errorCode,
)

class InvalidEnumValueException(
    message: String,
    val enumName: String,
    val invalidValue: String,
    val fieldName: String? = null,
) : AppException(message, HttpStatusCode.BadRequest, "INVALID_ENUM_VALUE")

class MissingParameterException(val parameterName: String) :
    AppException(Message.Errors.MISSING_PARAMETER.format(parameterName), HttpStatusCode.BadRequest, "MISSING_PARAMETER")

// ─── 401 Unauthorized ──────────────────────────────────────────────────────

class UnauthorizedException(message: String = Message.Errors.UNAUTHORIZED) :
    AppException(message, HttpStatusCode.Unauthorized, "UNAUTHORIZED")

class InvalidCredentialsException(
    remainingAttempts: Int? = null,
) : AppException(
        buildMessage(remainingAttempts),
        HttpStatusCode.Unauthorized,
        "INVALID_CREDENTIALS",
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

class UnverifiedAccountException(message: String = Message.Auth.ACCOUNT_NOT_VERIFIED) :
    AppException(message, HttpStatusCode.Unauthorized, "ACCOUNT_NOT_VERIFIED")

class DeactivatedAccountException(message: String = Message.Auth.ACCOUNT_DEACTIVATED) :
    AppException(message, HttpStatusCode.Unauthorized, "ACCOUNT_DEACTIVATED")

class MfaRequiredException(message: String = "MFA code required") :
    AppException(message, HttpStatusCode.Unauthorized, "MFA_REQUIRED")

class InvalidMfaCodeException(message: String = "Invalid MFA code") :
    AppException(message, HttpStatusCode.Unauthorized, "INVALID_MFA_CODE")

class FraudBlockedException(message: String = "Payment blocked by fraud rules") :
    AppException(message, HttpStatusCode.Forbidden, "FRAUD_BLOCKED")

// ─── 403 Forbidden ─────────────────────────────────────────────────────────

class ForbiddenException(message: String = Message.Errors.FORBIDDEN) :
    AppException(message, HttpStatusCode.Forbidden, "FORBIDDEN")

// ─── 404 Not Found ─────────────────────────────────────────────────────────

class NotFoundException(message: String = Message.Errors.NOT_FOUND) :
    AppException(message, HttpStatusCode.NotFound, "NOT_FOUND")

// ─── 409 Conflict ──────────────────────────────────────────────────────────

class ConflictException(message: String) : AppException(message, HttpStatusCode.Conflict, "CONFLICT")

// ─── 429 Too Many Requests ─────────────────────────────────────────────────

class RateLimitExceededException(message: String = "Too many requests") :
    AppException(message, HttpStatusCode.TooManyRequests, "RATE_LIMITED")

// ─── 500 Internal Server Error ─────────────────────────────────────────────

class InternalServerException(message: String = Message.Errors.INTERNAL) :
    AppException(message, HttpStatusCode.InternalServerError, "INTERNAL")

class DatabaseException(message: String) : AppException(message, HttpStatusCode.InternalServerError, "DATABASE_ERROR")

class EmailException(message: String) : AppException(message, HttpStatusCode.InternalServerError, "EMAIL_ERROR")
