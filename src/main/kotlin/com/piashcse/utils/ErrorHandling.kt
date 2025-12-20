package com.piashcse.utils

import io.ktor.http.*

/**
 * Custom exception classes for different error scenarios
 */
open class AppException(message: String, val code: HttpStatusCode = HttpStatusCode.BadRequest) : Exception(message)

class UnauthorizedException(message: String = "Unauthorized") : AppException(message, HttpStatusCode.Unauthorized)
class NotFoundException(message: String = "Not Found") : AppException(message, HttpStatusCode.NotFound)
class ValidationException(message: String = "Validation Error") : AppException(message, HttpStatusCode.BadRequest)
class ForbiddenException(message: String = "Forbidden") : AppException(message, HttpStatusCode.Forbidden)
class InternalServerErrorException(message: String = "Internal Server Error") : AppException(message, HttpStatusCode.InternalServerError)

/**
 * Validation utility functions
 */
object ValidationUtils {

    fun validateEmail(email: String): Boolean {
        return Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$").matches(email)
    }

    fun validatePassword(password: String): Boolean {
        // At least 8 characters, with at least one letter and one number
        return password.length >= 8 &&
               password.any { it.isLetter() } &&
               password.any { it.isDigit() }
    }
}