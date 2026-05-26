package com.piashcse.utils.extension

import com.piashcse.utils.common.ApiError
import com.piashcse.utils.validator.MissingParameterException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

// ============================================================================
//  REQUEST PARAMETER HELPERS
// ============================================================================

/**
 * Validates that all required parameters are present.
 * Throws MissingParameterException if any are missing (handled by StatusPages).
 */
@Deprecated(
    "Use Ktor 3.5.0 built-in parameter functions like requireQueryParameter or requirePathParameter instead.",
    level = DeprecationLevel.WARNING,
)
fun ApplicationCall.requireParameters(vararg requiredParams: String): List<String> {
    val missingParams = requiredParams.filterNot { parameters.contains(it) }
    if (missingParams.isNotEmpty()) {
        throw MissingParameterException(missingParams.first())
    }
    return requiredParams.map { parameters[it]!! }
}

fun ApplicationCall.paginateQueryParams(
    defaultLimit: Int = 20,
    defaultOffset: Int = 0,
    maxLimit: Int = 100,
): Pair<Int, Int> {
    val limit = (request.queryParameters["limit"]?.toIntOrNull() ?: defaultLimit).coerceAtMost(maxLimit)
    val offset = (request.queryParameters["offset"]?.toIntOrNull() ?: defaultOffset).coerceAtLeast(0)
    return limit to offset
}

/**
 * @deprecated Use requireParameters() instead. This function bypasses StatusPages.
 */
@Deprecated(
    "Use requireParameters() instead. This function bypasses StatusPages.",
    ReplaceWith("requireParameters(*requiredParams)"),
)
suspend fun ApplicationCall.requiredParameters(vararg requiredParams: String): List<String>? {
    val missingParams = requiredParams.filterNot { parameters.contains(it) }
    if (missingParams.isNotEmpty()) {
        respond(
            HttpStatusCode.BadRequest,
            ApiError("Missing parameters: ${missingParams.joinToString()}"),
        )
        return null
    }
    return requiredParams.map { parameters[it]!! }
}

fun String.fileExtension(): String {
    return this.substring(this.lastIndexOf("."))
}
