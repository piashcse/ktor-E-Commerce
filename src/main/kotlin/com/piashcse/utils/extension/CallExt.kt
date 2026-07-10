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
        else -> throw ValidationException(Message.Errors.invalidParameter(name, raw))
    }

    val limit = parseParam("limit", request.queryParameters["limit"], defaultLimit).coerceAtMost(maxLimit)
    val offset = parseParam("offset", request.queryParameters["offset"], defaultOffset).coerceAtLeast(0)
    return limit to offset
}

fun ApplicationCall.productWithFilterRequest(defaultLimit: Int = AppConstants.Pagination.DEFAULT_LIMIT): ProductWithFilterRequest {
    val (limit, offset) = paginateQueryParams(defaultLimit)
    return ProductWithFilterRequest(
        limit = limit,
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
