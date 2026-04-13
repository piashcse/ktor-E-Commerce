package com.piashcse.utils

import io.ktor.http.*

// ============================================================================
//  CENTRALIZED EXCEPTION HIERARCHY
//
//  Every business exception extends AppException:
//    - code        → HTTP status  (auto-used by StatusPages)
//    - errorCode   → machine-readable string for API consumers
//
//  To add a new exception: create a class below.
//  StatusPages needs ZERO changes — it catches AppException and reads
//  the status/errorCode directly from the exception.
// ============================================================================

/** Base exception for all application errors. */
open class AppException(
    message: String,
    val code: HttpStatusCode = HttpStatusCode.BadRequest,
    val errorCode: String = "BAD_REQUEST"
) : Exception(message)

// ─── HttpStatusCode.BadRequest ─────────────────────────────────────────────

class ValidationException(message: String, errorCode: String = "VALIDATION_ERROR")
    : AppException(message, HttpStatusCode.BadRequest, errorCode)

// ─── HttpStatusCode.Unauthorized ───────────────────────────────────────────

class UnauthorizedException(message: String = "Unauthorized", errorCode: String = "UNAUTHORIZED")
    : AppException(message, HttpStatusCode.Unauthorized, errorCode)

class InvalidCredentialsException(message: String = "Invalid email or password", errorCode: String = "INVALID_CREDENTIALS")
    : AppException(message, HttpStatusCode.Unauthorized, errorCode)

class UnverifiedAccountException(message: String = "Account is not verified", errorCode: String = "UNVERIFIED_ACCOUNT")
    : AppException(message, HttpStatusCode.Unauthorized, errorCode)

class DeactivatedAccountException(message: String = "Account has been deactivated", errorCode: String = "DEACTIVATED_ACCOUNT")
    : AppException(message, HttpStatusCode.Unauthorized, errorCode)

// ─── HttpStatusCode.Forbidden ──────────────────────────────────────────────

class ForbiddenException(message: String = "Insufficient permissions", errorCode: String = "FORBIDDEN")
    : AppException(message, HttpStatusCode.Forbidden, errorCode)

// ─── HttpStatusCode.NotFound ───────────────────────────────────────────────

class NotFoundException(message: String, errorCode: String = "NOT_FOUND")
    : AppException(message, HttpStatusCode.NotFound, errorCode)

// ─── HttpStatusCode.Conflict ───────────────────────────────────────────────

class ConflictException(message: String, errorCode: String = "CONFLICT")
    : AppException(message, HttpStatusCode.Conflict, errorCode)

// ─── HttpStatusCode.TooManyRequests ────────────────────────────────────────

class RateLimitExceededException(message: String = "Too many requests", errorCode: String = "RATE_LIMITED")
    : AppException(message, HttpStatusCode.TooManyRequests, errorCode)

// ─── HttpStatusCode.InternalServerError ────────────────────────────────────

class InternalServerException(message: String = "Internal server error", errorCode: String = "INTERNAL_ERROR")
    : AppException(message, HttpStatusCode.InternalServerError, errorCode)

// ─── Business logic exceptions (domain-specific) ──────────────────────────

/** For invalid enum values (e.g., invalid order status, user type) */
class InvalidEnumValueException(
    message: String,
    errorCode: String = "INVALID_ENUM_VALUE",
    val enumName: String,
    val invalidValue: String
) : AppException(message, HttpStatusCode.BadRequest, errorCode)

/** For missing required parameters (replaces direct response in routes) */
class MissingParameterException(parameterName: String)
    : AppException("Missing required parameter: $parameterName", HttpStatusCode.BadRequest, "MISSING_PARAMETER")

/** For database operation failures */
class DatabaseException(message: String, errorCode: String = "DATABASE_ERROR")
    : AppException(message, HttpStatusCode.InternalServerError, errorCode)

// ============================================================================
//  VALIDATION FACTORS — one-liners that throw typed exceptions
//  Usage:  requireNotBlank(userId, "User ID")
//          requirePositive(quantity, "Quantity")
// ============================================================================

fun requireNotBlank(value: String, fieldName: String) {
    if (value.isBlank()) throw ValidationException("$fieldName cannot be blank", "BLANK_FIELD")
}

fun requirePositive(value: Number, fieldName: String) {
    if (value.toDouble() <= 0) throw ValidationException("$fieldName must be greater than 0", "NOT_POSITIVE")
}

fun requireNonNegative(value: Number, fieldName: String) {
    if (value.toDouble() < 0) throw ValidationException("$fieldName cannot be negative", "NEGATIVE_VALUE")
}

fun requireValidEmail(email: String) {
    val regex = Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")
    if (!regex.matches(email)) throw ValidationException("Invalid email format", "INVALID_EMAIL")
}

fun requireValidPassword(password: String, minLength: Int = 8) {
    if (password.length < minLength) throw ValidationException("Password must be at least $minLength characters", "WEAK_PASSWORD")
}

// ============================================================================
//  STRING EXTENSIONS — concise throw syntax in service/repository code
//  Usage:  productId.throwNotFound("Product")
//          brandName.throwConflict("Brand")
// ============================================================================

/** Throw NotFoundException with auto-generated error code. */
fun String.throwNotFound(resourceName: String = "Resource"): Nothing =
    throw NotFoundException("$resourceName not found: $this", "${resourceName.uppercase()}_NOT_FOUND")

/** Throw ConflictException with auto-generated error code. */
fun String.throwConflict(resourceName: String = "Resource"): Nothing =
    throw ConflictException("$resourceName already exists: $this", "${resourceName.uppercase()}_EXISTS")

/** Throw InvalidEnumValueException with auto-generated error code. */
fun String.throwInvalidEnumValue(enumName: String, resourceName: String = "Value"): Nothing =
    throw InvalidEnumValueException(
        message = "Invalid $resourceName: $this. Must be one of $enumName",
        enumName = enumName,
        invalidValue = this
    )
