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
    val errors: List<FieldError>? = null,
)

/**
 * Structured field-level validation error (only for validation failures).
 */
@Serializable
data class FieldError(
    val field: String,
    val message: String,
)

/** Convert any AppException → (HttpStatusCode, ApiError) pair. */
fun AppException.toErrorResponse(): Pair<HttpStatusCode, ApiError> = code to ApiError(message ?: "Unknown error")
