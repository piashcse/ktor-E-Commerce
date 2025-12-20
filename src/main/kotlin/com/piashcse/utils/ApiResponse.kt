package com.piashcse.utils

import io.ktor.http.*

data class Response(
    val isSuccess: Boolean,
    val statusCode: HttpStatusCode? = null,
    val data: Any? = null,
    val error: Any? = null
)

/**
 * Sealed class for representing API responses
 */
sealed class DetailedApiResponse<out T> {
    data class Success<T>(val data: T, val message: String = "Success", val code: HttpStatusCode = HttpStatusCode.OK) : DetailedApiResponse<T>()
    data class Error(val message: String, val code: HttpStatusCode = HttpStatusCode.BadRequest) : DetailedApiResponse<Nothing>()
}

object ApiResponse {
    fun <T> success(data: T, statsCode: HttpStatusCode?) = Response(true, data = data, statusCode = statsCode)
    fun <T> failure(error: T, statsCode: HttpStatusCode?) = Response(false, error = error, statusCode = statsCode)

    /**
     * New structure for more detailed API responses
     */
    fun <T> successDetailed(data: T, message: String = "Success", code: HttpStatusCode = HttpStatusCode.OK): DetailedApiResponse<T> {
        return DetailedApiResponse.Success(data, message, code)
    }

    fun errorDetailed(message: String, code: HttpStatusCode = HttpStatusCode.BadRequest): DetailedApiResponse<Nothing> {
        return DetailedApiResponse.Error(message, code)
    }
}

/**
 * Extension functions for easy response creation
 */
fun <T> successResponse(data: T, message: String = "Success", code: HttpStatusCode = HttpStatusCode.OK) =
    ApiResponse.successDetailed(data, message, code)

fun errorResponse(message: String, code: HttpStatusCode = HttpStatusCode.BadRequest) =
    ApiResponse.errorDetailed(message, code)

fun <T> AppException.toErrorResponse(): DetailedApiResponse<Nothing> =
    errorResponse(this.message ?: "An error occurred", this.code)
