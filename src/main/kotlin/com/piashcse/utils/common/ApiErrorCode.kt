package com.piashcse.utils.common

/**
 * Machine-readable error codes returned in [ApiError.code].
 *
 * Clients should branch on these values, not on human-readable messages.
 */
object ApiErrorCode {
    const val BAD_REQUEST = "BAD_REQUEST"
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val FORBIDDEN = "FORBIDDEN"
    const val NOT_FOUND = "NOT_FOUND"
    const val CONFLICT = "CONFLICT"
    const val VALIDATION_FAILED = "VALIDATION_FAILED"
    const val SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE"
    const val INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR"
    const val DEFAULT = "DEFAULT"
}
