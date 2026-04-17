package com.piashcse.utils

import io.ktor.http.*

/**
 * Industry-standard API error response (used ONLY for errors).
 * 
 * Based on Stripe, GitHub, OpenAI standards:
 * - Success: Return data directly (NO wrapper)
 * - Error: Return ApiError with message (and errors array for validation)
 */
data class ApiError(
    val message: String,
    val errors: List<FieldError>? = null
)

/**
 * Structured field-level validation error (only for validation failures).
 */
data class FieldError(
    val field: String,
    val message: String
)

/** Convert any AppException → (HttpStatusCode, ApiError) pair. */
fun AppException.toErrorResponse(): Pair<HttpStatusCode, ApiError> =
    code to ApiError(message ?: "Unknown error")

