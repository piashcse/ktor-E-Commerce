package com.piashcse.utils.extension

import com.piashcse.config.DotEnv
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.model.request.NameRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.utils.common.CursorRequest
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.common.PageRequest
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import java.time.Instant

suspend inline fun <reified T : Any> ApplicationCall.respondOk(data: T) = respond(HttpStatusCode.OK, data)

suspend inline fun <reified T : Any> ApplicationCall.respondCreated(data: T) = respond(HttpStatusCode.Created, data)

/** Standard delete acknowledgement so every module returns the same shape. */
suspend fun ApplicationCall.respondDeleted(message: String) = respondMessage(message)

/** Standard success acknowledgement for actions that have no response body. */
suspend fun ApplicationCall.respondMessage(message: String) = respondOk(MessageResponse(message))

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

/**
 * Extracts a Bearer credential exactly the way Ktor's auth providers do:
 * scheme comparison is case-insensitive and extra whitespace is ignored.
 * Using `startsWith("Bearer ")` + fixed `substring(7)` bypasses blacklist
 * checks for `bearer ...` / `Bearer   ...` headers while Ktor still accepts them.
 */
fun ApplicationCall.bearerToken(): String? {
    val header = request.headers[HttpHeaders.Authorization]?.trim() ?: return null
    val parts = header.split(Regex("\\s+"), limit = 2)
    if (parts.size != 2 || !parts[0].equals("Bearer", ignoreCase = true)) return null
    return parts[1].trim().takeIf { it.isNotEmpty() }
}

/** Typed query accessors: centralize parsing so routes stay one-liners. */
fun ApplicationCall.queryBooleanOrNull(name: String): Boolean? =
    request.queryParameters[name]?.let { raw ->
        raw.toBooleanStrictOrNull() ?: throw ValidationException(Message.Errors.invalidParameter(name, raw))
    }

/** ISO-8601 instant query param; malformed values surface as a validation error. */
fun ApplicationCall.queryInstant(name: String): Instant? =
    request.queryParameters[name]?.takeIf { it.isNotBlank() }?.let { raw ->
        runCatching { Instant.parse(raw) }.getOrElse {
            throw ValidationException(Message.Errors.invalidParameter(name, raw))
        }
    }

/** ISO local-date query param (`yyyy-MM-dd`). */
fun ApplicationCall.queryLocalDate(name: String): java.time.LocalDate? =
    request.queryParameters[name]?.takeIf { it.isNotBlank() }?.let { raw ->
        runCatching { java.time.LocalDate.parse(raw) }.getOrElse {
            throw ValidationException(Message.Errors.invalidParameter(name, raw))
        }
    }

fun ApplicationCall.queryBoolean(
    name: String,
    defaultValue: Boolean = false,
): Boolean = queryBooleanOrNull(name) ?: defaultValue

/**
 * Normalized offset pagination.
 *
 * `page`/`perPage` take precedence when present; legacy `limit`/`offset` are
 * still accepted. Malformed or out-of-range values fail with a validation error
 * instead of silently falling back, so every route behaves identically.
 */
fun ApplicationCall.pageRequest(
    defaultPerPage: Int = AppConstants.Pagination.DEFAULT_LIMIT,
    defaultPage: Int = 1,
    maxPerPage: Int = AppConstants.Pagination.MAX_LIMIT,
): PageRequest {
    val hasPageParams = request.queryParameters["page"] != null || request.queryParameters["perPage"] != null
    if (hasPageParams) {
        val perPage = intParam("perPage", defaultPerPage).coerceIn(1, maxPerPage)
        val page = intParam("page", defaultPage).coerceAtLeast(1)
        return PageRequest(
            limit = perPage,
            offset = (((page.toLong() - 1) * perPage).coerceIn(0, AppConstants.Pagination.MAX_OFFSET.toLong())).toInt(),
        )
    }
    return PageRequest(
        limit = intParam("limit", defaultPerPage).coerceIn(1, maxPerPage),
        offset =
            intParam("offset", AppConstants.Pagination.DEFAULT_OFFSET)
                .coerceIn(0, AppConstants.Pagination.MAX_OFFSET),
    )
}

/** Keyset pagination request: `limit` (or `perPage`) plus an opaque `cursor`. */
fun ApplicationCall.cursorRequest(
    defaultLimit: Int = AppConstants.Pagination.DEFAULT_LIMIT,
    maxLimit: Int = AppConstants.Pagination.MAX_LIMIT,
): CursorRequest =
    CursorRequest(
        limit = pageRequest(defaultPerPage = defaultLimit, maxPerPage = maxLimit).limit,
        cursor = request.queryParameters["cursor"]?.takeIf { it.isNotBlank() },
    )

/** Validated, clamped `limit` for non-paginated top-N endpoints. */
fun ApplicationCall.queryLimit(
    name: String = "limit",
    default: Int = AppConstants.Pagination.DEFAULT_LIMIT,
    max: Int = AppConstants.Pagination.MAX_LIMIT,
): Int = intParam(name, default).coerceIn(1, max)

private fun ApplicationCall.intParam(
    name: String,
    default: Int,
): Int =
    request.queryParameters[name]?.let { raw ->
        raw.toIntOrNull() ?: throw ValidationException(Message.Errors.invalidParameter(name, raw))
    } ?: default

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
    val page = pageRequest(defaultPerPage)
    return ProductWithFilterRequest(
        limit = page.limit,
        offset = page.offset,
        maxPrice = request.queryParameters["maxPrice"]?.toDoubleOrNull(),
        minPrice = request.queryParameters["minPrice"]?.toDoubleOrNull(),
        categoryId = request.queryParameters["categoryId"],
        subCategoryId = request.queryParameters["subCategoryId"],
        brandId = request.queryParameters["brandId"],
        shopId = request.queryParameters["shopId"],
        minRating = request.queryParameters["minRating"]?.toDoubleOrNull(),
        inStockOnly = queryBooleanOrNull("inStockOnly"),
        freeShipping = queryBooleanOrNull("freeShipping"),
        onSale = queryBooleanOrNull("onSale"),
        hotDeal = queryBooleanOrNull("hotDeal"),
        featured = queryBooleanOrNull("featured"),
        sortBy = request.queryParameters["sortBy"],
        sortOrder = request.queryParameters["sortOrder"],
    )
}
