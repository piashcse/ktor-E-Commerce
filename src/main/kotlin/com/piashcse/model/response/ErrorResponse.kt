package com.piashcse.model.response

import kotlinx.serialization.Serializable

/**
 * Standardized error response for API errors.
 * Note: FieldError is defined in com.piashcse.utils.ApiResponse to avoid duplication.
 */
@Serializable
data class ErrorResponse(
    val message: String,
    val errorCode: String? = null
)
