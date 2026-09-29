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
    val code: String = "BAD_REQUEST",
    val errors: List<FieldError>? = null,
    val requestId: String? = null,
    // RFC 9457 / Spring-Boot compat aliases — additive only, message/code stay canonical.
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
)

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
    code to
        ApiError(
            message = message ?: "Unknown error",
            code = errorCode,
            requestId = requestId,
            type = errorCode,
            title = message,
            status = code.value,
        )

/** Standard success message envelope — replaces ad-hoc mapOf("message" to ...) for OpenAPI schema. */
@Serializable
data class MessageResponse(val message: String)
