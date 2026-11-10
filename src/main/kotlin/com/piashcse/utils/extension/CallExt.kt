package com.piashcse.utils.extension

import com.piashcse.config.DotEnv
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.model.request.NameRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*

suspend inline fun <reified T : Any> ApplicationCall.respondOk(data: T) = respond(HttpStatusCode.OK, data)

suspend inline fun <reified T : Any> ApplicationCall.respondCreated(data: T) = respond(HttpStatusCode.Created, data)

/** Standard delete acknowledgement so every module returns the same shape. */
suspend fun ApplicationCall.respondDeleted(message: String) = respondOk(MessageResponse(message))

/**
 * Best-effort client IP.
 *
 * Proxy headers are attacker-controlled unless the app actually runs behind a
 * trusted proxy. Only when `TRUST_PROXY=true` (read through DotEnv so process
 * env and .env agree) do we honor `X-Forwarded-For` / `X-Real-IP`. Otherwise the
 * socket address is used, which prevents rate-limit bypass by header spoofing.
 */
val ApplicationCall.clientIp: String
    get() {
        val trustProxy = runCatching { DotEnv.getBoolean("TRUST_PROXY", false) }.getOrDefault(false)
        if (trustProxy) {
            request.headers[HttpHeaders.XForwardedFor]
                ?.substringBefore(',')
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
            request.headers["X-Real-IP"]?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return request.local.remoteHost
    }

/** Typed query accessors: centralize parsing so routes stay one-liners. */
fun ApplicationCall.queryInt(name: String): Int? = request.queryParameters[name]?.toIntOrNull()

fun ApplicationCall.queryBoolean(
    name: String,
    defaultValue: Boolean = false,
): Boolean = request.queryParameters[name]?.toBooleanStrictOrNull() ?: defaultValue

fun ApplicationCall.paginateQueryParams(
    defaultPerPage: Int = AppConstants.Pagination.DEFAULT_LIMIT,
    defaultPage: Int = 1,
    maxPerPage: Int = AppConstants.Pagination.MAX_LIMIT,
): Pair<Int, Int> {
    fun parseParam(
        name: String,
        raw: String?,
        default: Int,
    ): Int =
        when {
            raw == null -> default
            raw.toIntOrNull() != null -> raw.toInt()
            else -> throw ValidationException(Message.Errors.invalidParameter(name, raw))
        }

    val perPage =
        parseParam("perPage", request.queryParameters["perPage"], defaultPerPage)
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

/**
 * Stripe-style idempotency key: `Idempotency-Key` header wins, body field is fallback.
 * Returns null when neither is present.
 */
fun ApplicationCall.idempotencyKey(bodyKey: String? = null): String? =
    request.headers["Idempotency-Key"]?.takeIf { it.isNotBlank() } ?: bodyKey?.takeIf { it.isNotBlank() }

/**
 * Catalog create/update compatibility: `?name=` wins, otherwise read the JSON
 * body. Keeps the old query API working while the docs/examples use JSON.
 */
suspend fun ApplicationCall.nameFromBodyOrQuery(): String =
    request.queryParameters["name"]?.takeIf { it.isNotBlank() }
        ?: runCatching { receive<NameRequest>().name }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: throw ValidationException("name is required")

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
