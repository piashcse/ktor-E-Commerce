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
    fun parseParam(
        name: String,
        raw: String?,
        default: Int,
    ): Int = when {
        raw == null -> default
        raw.toIntOrNull() != null -> raw.toInt()
        else -> throw ValidationException("Invalid $name: $raw")
    }

    val limit = parseParam("limit", request.queryParameters["limit"], defaultLimit).coerceAtMost(maxLimit)
    val offset = parseParam("offset", request.queryParameters["offset"], defaultOffset).coerceAtLeast(0)
    return limit to offset
}
