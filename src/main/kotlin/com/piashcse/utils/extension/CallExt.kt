package com.piashcse.utils.extension

import io.ktor.http.*
import io.ktor.server.application.*

fun ApplicationCall.paginateQueryParams(
    defaultLimit: Int = 20,
    defaultOffset: Int = 0,
    maxLimit: Int = 100,
): Pair<Int, Int> {
    val limit = (request.queryParameters["limit"]?.toIntOrNull() ?: defaultLimit).coerceAtMost(maxLimit)
    val offset = (request.queryParameters["offset"]?.toIntOrNull() ?: defaultOffset).coerceAtLeast(0)
    return limit to offset
}
