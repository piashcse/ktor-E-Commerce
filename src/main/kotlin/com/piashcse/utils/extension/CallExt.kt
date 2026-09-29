package com.piashcse.utils.extension

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

suspend inline fun <reified T : Any> ApplicationCall.respondOk(data: T) = respond(HttpStatusCode.OK, data)
suspend inline fun <reified T : Any> ApplicationCall.respondCreated(data: T) = respond(HttpStatusCode.Created, data)

/**
 * Best-effort client IP. X-Forwarded-For is attacker-controlled unless the app
 * runs behind a trusted proxy (TRUST_PROXY=true). By default only X-Real-IP
 * from infra + socket host are used; enable TRUST_PROXY only behind LB/CDN
 * that sanitizes XFF.
 */
val ApplicationCall.clientIp: String
    get() {
        val trustProxy = System.getenv("TRUST_PROXY")?.equals("true", ignoreCase = true) == true
        if (trustProxy) {
            request.headers[HttpHeaders.XForwardedFor]?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }
        return request.headers["X-Real-IP"]?.takeIf { it.isNotEmpty() }
            ?: request.local.remoteHost
    }

fun ApplicationCall.paginateQueryParams(
    defaultPerPage: Int = AppConstants.Pagination.DEFAULT_LIMIT,
    defaultPage: Int = 1,
    maxPerPage: Int = AppConstants.Pagination.MAX_LIMIT,
): Pair<Int, Int> {
    fun parseParam(name: String, raw: String?, default: Int): Int = when {
        raw == null -> default
        raw.toIntOrNull() != null -> raw.toInt()
        else -> throw ValidationException(Message.Errors.invalidParameter(name, raw))
    }

    val perPage = parseParam("perPage", request.queryParameters["perPage"], defaultPerPage)
        .coerceAtMost(maxPerPage).coerceAtLeast(1)
    val page = parseParam("page", request.queryParameters["page"], defaultPage).coerceAtLeast(1)

    // Support legacy limit/offset params, but page/perPage take precedence
    val legacyLimit = request.queryParameters["limit"]?.toIntOrNull()
    val legacyOffset = request.queryParameters["offset"]?.toIntOrNull()
    // Cap offset to prevent deep-offset DoS (large OFFSET scans).
    val maxOffset = 10_000
    return if (legacyLimit != null || legacyOffset != null) {
        (legacyLimit ?: perPage).coerceAtMost(maxPerPage).coerceAtLeast(1) to
            (legacyOffset ?: 0).coerceAtLeast(0).coerceAtMost(maxOffset)
    } else {
        perPage to ((page - 1) * perPage).coerceAtMost(maxOffset)
    }
}

fun ApplicationCall.productWithFilterRequest(defaultPerPage: Int = AppConstants.Pagination.DEFAULT_LIMIT): ProductWithFilterRequest {
    val (perPage, offset) = paginateQueryParams(defaultPerPage)
    return ProductWithFilterRequest(
        limit = perPage,
        offset = offset,
        maxPrice = request.queryParameters["maxPrice"]?.toDoubleOrNull(),
        minPrice = request.queryParameters["minPrice"]?.toDoubleOrNull(),
        categoryId = request.queryParameters["categoryId"],
        subCategoryId = request.queryParameters["subCategoryId"],
        brandId = request.queryParameters["brandId"],
        sortBy = request.queryParameters["sortBy"],
        sortOrder = request.queryParameters["sortOrder"],
    )
}
