package helpers

import io.ktor.http.HttpStatusCode

data class Response(val isSuccess: Boolean, val statsCode : HttpStatusCode? = null, val data: Any? = null, val error: Any? = null)

object JsonResponse {
    fun <T> success(data: T, statsCode: HttpStatusCode?) = Response(true, data = data, statsCode = statsCode)
    fun <T> failure(error: T, statsCode: HttpStatusCode?) = Response(false, error = error, statsCode = statsCode)
}
