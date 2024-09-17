package com.piashcse.utils

import io.ktor.http.*

data class Response(
    val isSuccess: Boolean,
    val statusCode: HttpStatusCode? = null,
    val data: Any? = null,
    val error: Any? = null
)

object ApiResponse {
    fun <T> success(data: T, statsCode: HttpStatusCode?) = Response(true, data = data, statusCode = statsCode)
    fun <T> failure(error: T, statsCode: HttpStatusCode?) = Response(false, error = error, statusCode = statsCode)
}
