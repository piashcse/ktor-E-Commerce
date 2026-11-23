package com.piashcse.utils.common

import com.piashcse.utils.validator.AppException
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Industry-standard API error response (used ONLY for errors).
 *
 * Based on Stripe, GitHub, OpenAI standards:
 * - Success: Return data directly (NO wrapper)
 * - Error: Return ApiError with message (and errors array for validation)
 */
@Serializable
data class ApiError(
    val message: String,
    val code: String = ApiErrorCode.BAD_REQUEST,
    val errors: List<FieldError>? = null,
    val requestId: String? = null,
    // RFC 9457 / Spring-Boot compat aliases — additive only, message/code stay canonical.
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
) {
    companion object {
        /** Build the canonical error envelope with RFC 9457 aliases filled in. */
        fun of(
            message: String,
            code: String = ApiErrorCode.BAD_REQUEST,
            status: HttpStatusCode = HttpStatusCode.BadRequest,
            requestId: String? = null,
            errors: List<FieldError>? = null,
        ): ApiError = ApiError(message, code, errors, requestId, code, message, status.value)
    }
}

/**
 * Structured field-level validation error (only for validation failures).
 */
@Serializable
data class FieldError(
    val field: String,
    val message: String,
)

/** Convert any AppException → (HttpStatusCode, ApiError) pair. Pass call.requestId() to correlate. */
fun AppException.toErrorResponse(requestId: String? = null): Pair<HttpStatusCode, ApiError> =
    code to ApiError.of(message ?: "Unknown error", errorCode, code, requestId)

/** Standard success message envelope — replaces ad-hoc mapOf("message" to ...) for OpenAPI schema. */
@Serializable
data class MessageResponse(val message: String)
