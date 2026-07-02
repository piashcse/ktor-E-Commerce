package com.piashcse.utils.extension

import com.piashcse.utils.validator.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*

fun ApplicationCall.paginateQueryParams(
    defaultLimit: Int = 20,
    defaultOffset: Int = 0,
    maxLimit: Int = 100,
): Pair<Int, Int> {
    val limitParam = request.queryParameters["limit"]
    val offsetParam = request.queryParameters["offset"]
    val limit = limitParam?.toIntOrNull() ?: limitParam?.let { throw ValidationException("Invalid limit: $it") } ?: defaultLimit
    val offset = offsetParam?.toIntOrNull() ?: offsetParam?.let { throw ValidationException("Invalid offset: $it") } ?: defaultOffset
    return (limit.coerceAtMost(maxLimit)) to (offset.coerceAtLeast(0))
}
