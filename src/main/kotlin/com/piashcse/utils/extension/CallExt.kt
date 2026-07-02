package com.piashcse.utils.extension

import com.piashcse.constants.AppConstants
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*

fun ApplicationCall.paginateQueryParams(
    defaultLimit: Int = AppConstants.Pagination.DEFAULT_LIMIT,
    defaultOffset: Int = AppConstants.Pagination.DEFAULT_OFFSET,
    maxLimit: Int = AppConstants.Pagination.MAX_LIMIT,
): Pair<Int, Int> {
    val limitParam = request.queryParameters["limit"]
    val offsetParam = request.queryParameters["offset"]
    val limit = limitParam?.toIntOrNull() ?: limitParam?.let { throw ValidationException("Invalid limit: $it") } ?: defaultLimit
    val offset = offsetParam?.toIntOrNull() ?: offsetParam?.let { throw ValidationException("Invalid offset: $it") } ?: defaultOffset
    return (limit.coerceAtMost(maxLimit)) to (offset.coerceAtLeast(0))
}
