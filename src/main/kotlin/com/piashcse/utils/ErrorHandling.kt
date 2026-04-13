package com.piashcse.utils

import com.piashcse.constants.Message
import io.ktor.http.*

// ============================================================================
//  INDUSTRY-STANDARD EXCEPTION HIERARCHY
//
//  Based on Stripe, GitHub, OpenAI best practices:
//  - HTTP status code is the ONLY source of truth
//  - No error codes in response body
//  - Clear, user-facing messages
//  - Minimal, focused hierarchy
//
//  To add new exception: create class extending AppException
//  StatusPages auto-handles it - no configuration needed
// ============================================================================

/** Base exception for all application errors. */
open class AppException(
    message: String,
    val code: HttpStatusCode = HttpStatusCode.BadRequest
) : Exception(message)

// ─── 400 Bad Request ───────────────────────────────────────────────────────

class ValidationException(message: String) : AppException(message, HttpStatusCode.BadRequest)

class InvalidEnumValueException(
    message: String,
    val enumName: String,
    val invalidValue: String
) : AppException(message, HttpStatusCode.BadRequest)

class MissingParameterException(parameterName: String)
    : AppException("Missing required parameter: $parameterName", HttpStatusCode.BadRequest)

// ─── 401 Unauthorized ──────────────────────────────────────────────────────

class UnauthorizedException(message: String = Message.Errors.UNAUTHORIZED)
    : AppException(message, HttpStatusCode.Unauthorized)

class InvalidCredentialsException(message: String = Message.Auth.INVALID_CREDENTIALS)
    : AppException(message, HttpStatusCode.Unauthorized)

class UnverifiedAccountException(message: String = Message.Auth.ACCOUNT_NOT_VERIFIED)
    : AppException(message, HttpStatusCode.Unauthorized)

class DeactivatedAccountException(message: String = Message.Auth.ACCOUNT_DEACTIVATED)
    : AppException(message, HttpStatusCode.Unauthorized)

// ─── 403 Forbidden ─────────────────────────────────────────────────────────

class ForbiddenException(message: String = Message.Errors.FORBIDDEN)
    : AppException(message, HttpStatusCode.Forbidden)

// ─── 404 Not Found ─────────────────────────────────────────────────────────

class NotFoundException(message: String = Message.Errors.NOT_FOUND)
    : AppException(message, HttpStatusCode.NotFound)

// ─── 409 Conflict ──────────────────────────────────────────────────────────

class ConflictException(message: String) : AppException(message, HttpStatusCode.Conflict)

// ─── 429 Too Many Requests ─────────────────────────────────────────────────

class RateLimitExceededException(message: String = "Too many requests")
    : AppException(message, HttpStatusCode.TooManyRequests)

// ─── 500 Internal Server Error ─────────────────────────────────────────────

class InternalServerException(message: String = Message.Errors.INTERNAL)
    : AppException(message, HttpStatusCode.InternalServerError)

class DatabaseException(message: String) : AppException(message, HttpStatusCode.InternalServerError)

// ============================================================================
//  VALIDATION HELPERS - Uses Message constants for consistency
//  Usage:  requireNotBlank(userId, "User ID")
//          requirePositive(quantity, "Quantity")
// ============================================================================

fun requireNotBlank(value: String, fieldName: String) {
    if (value.isBlank()) throw ValidationException(Message.Validation.blankField(fieldName))
}

fun requirePositive(value: Number, fieldName: String) {
    if (value.toDouble() <= 0) throw ValidationException(Message.Validation.notPositive(fieldName))
}

fun requireNonNegative(value: Number, fieldName: String) {
    if (value.toDouble() < 0) throw ValidationException(Message.Validation.negativeValue(fieldName))
}

fun requireValidEmail(email: String) {
    val regex = Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")
    if (!regex.matches(email)) throw ValidationException(Message.Validation.INVALID_EMAIL)
}

fun requireValidPassword(password: String, minLength: Int = 8) {
    if (password.length < minLength) throw ValidationException(Message.Validation.WEAK_PASSWORD)
}

// ============================================================================
//  STRING EXTENSIONS — concise throw syntax for common patterns
//  Usage:  productId.throwNotFound("Product")
//          email.throwConflict("User")
// ============================================================================

/** Throw NotFoundException with specific entity type */
fun String.throwNotFound(resourceName: String): Nothing =
    throw NotFoundException("$resourceName not found")

/** Throw NotFoundException with default message */
fun String.throwNotFound(): Nothing =
    throw NotFoundException()

/** Throw ConflictException */
fun String.throwConflict(resourceName: String): Nothing =
    throw ConflictException("$resourceName already exists")

