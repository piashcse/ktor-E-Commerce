package com.piashcse.utils

/**
 * Centralized error code registry for all API errors.
 * All error codes follow the pattern: UPPER_SNAKE_CASE
 */
object ErrorCodes {
    // Client errors (4xx)
    const val BAD_REQUEST = "BAD_REQUEST"
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val FORBIDDEN = "FORBIDDEN"
    const val NOT_FOUND = "NOT_FOUND"
    const val CONFLICT = "CONFLICT"
    const val VALIDATION_ERROR = "VALIDATION_ERROR"
    const val INVALID_ENUM_VALUE = "INVALID_ENUM_VALUE"
    const val MISSING_PARAMETER = "MISSING_PARAMETER"
    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    const val UNVERIFIED_ACCOUNT = "UNVERIFIED_ACCOUNT"
    const val DEACTIVATED_ACCOUNT = "DEACTIVATED_ACCOUNT"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val BLANK_FIELD = "BLANK_FIELD"
    const val INVALID_EMAIL = "INVALID_EMAIL"
    const val WEAK_PASSWORD = "WEAK_PASSWORD"
    const val INVALID_NUMBER = "INVALID_NUMBER"
    const val INVALID_ARGUMENT = "INVALID_ARGUMENT"
    
    // Server errors (5xx)
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
    const val DATABASE_ERROR = "DATABASE_ERROR"
    const val EMAIL_SEND_FAILED = "EMAIL_SEND_FAILED"
    
    // Generic
    const val ERROR = "ERROR"
    const val UNKNOWN = "UNKNOWN"
}

/**
 * Centralized error messages for consistent user-facing messages.
 */
object ErrorMessages {
    const val UNAUTHORIZED = "Authentication required"
    const val FORBIDDEN = "Insufficient permissions"
    const val INTERNAL_ERROR = "Internal server error"
    const val UNKNOWN = "Unknown error occurred"
    const val BAD_REQUEST = "Bad request"
    const val NOT_FOUND = "Resource not found"
}
