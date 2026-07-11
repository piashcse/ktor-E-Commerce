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
    return if (legacyLimit != null || legacyOffset != null) {
        (legacyLimit ?: perPage).coerceAtMost(maxPerPage).coerceAtLeast(1) to
            (legacyOffset ?: 0).coerceAtLeast(0)
    } else {
        perPage to (page - 1) * perPage
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
