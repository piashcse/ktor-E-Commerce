package com.piashcse.utils

import io.ktor.http.*
import java.time.Instant

/**
 * Industry-standard API response envelope.
 *
 * Based on Stripe, GitHub, OpenAI, and major REST API standards:
 * - Success: { "success": true, "data": {...} }
 * - Error: { "success": false, "message": "..." } (HTTP status code is the source of truth)
 * - Validation errors include structured field-level details
 *
 * No redundant metadata (path, timestamp, code) - HTTP headers and status code already provide this.
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val errors: List<FieldError>? = null
) {
    companion object {
        // ── Success ───────────────────────────────────────────────────────
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)
        fun okMessage(message: String): ApiResponse<Nothing> = ApiResponse(success = true, message = message)

        // ── Error factories (minimal, HTTP status is the source of truth) ─
        fun badRequest(message: String): ApiResponse<Nothing> =
            ApiResponse(success = false, message = message)

        fun unauthorized(message: String = ErrorMessages.UNAUTHORIZED): ApiResponse<Nothing> =
            ApiResponse(success = false, message = message)

        fun forbidden(message: String = ErrorMessages.FORBIDDEN): ApiResponse<Nothing> =
            ApiResponse(success = false, message = message)

        fun notFound(message: String): ApiResponse<Nothing> =
            ApiResponse(success = false, message = message)

        fun conflict(message: String): ApiResponse<Nothing> =
            ApiResponse(success = false, message = message)

        fun validationError(message: String = "Validation failed", errors: List<FieldError>? = null): ApiResponse<Nothing> =
            ApiResponse(success = false, message = message, errors = errors)

        fun internalError(message: String = ErrorMessages.INTERNAL_ERROR): ApiResponse<Nothing> =
            ApiResponse(success = false, message = message)

        fun fromException(ex: AppException): ApiResponse<Nothing> =
            ApiResponse(success = false, message = ex.message ?: ErrorMessages.UNKNOWN)

        fun error(message: String): ApiResponse<Nothing> =
            ApiResponse(success = false, message = message)

        // ── Legacy (backward compat) ──────────────────────────────────────
        @Deprecated("Use ok(data)", ReplaceWith("ok(data)"))
        fun <T> success(data: T, statusCode: HttpStatusCode? = null): ApiResponse<T> = ok(data)

        @Deprecated("Use okMessage(msg)", ReplaceWith("okMessage(message)"))
        fun success(message: String, statusCode: HttpStatusCode? = null): ApiResponse<Nothing> = okMessage(message)

        @Deprecated("Use badRequest(msg)", ReplaceWith("badRequest(message)"))
        fun <T> failure(message: T, statusCode: HttpStatusCode? = null): ApiResponse<Nothing> =
            ApiResponse(success = false, message = message.toString())

        @Deprecated("Use error(msg)", ReplaceWith("error(message)"))
        fun failureWithStatus(message: String, statusCode: HttpStatusCode): ApiResponse<Nothing> =
            ApiResponse(success = false, message = message)
    }
}

/**
 * Structured field-level validation error (only present when errors has items).
 */
data class FieldError(
    val field: String,
    val message: String
)

/** Convert any AppException → (HttpStatusCode, ApiResponse) pair. */
fun AppException.toResponse(): Pair<HttpStatusCode, ApiResponse<Nothing>> =
    code to ApiResponse.fromException(this)
