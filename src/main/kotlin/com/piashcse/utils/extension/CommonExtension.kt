package com.piashcse.utils.extension

import com.piashcse.constants.UserType
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.utils.MissingParameterException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

// ============================================================================
//  DATABASE QUERY HELPER
// ============================================================================

/** Execute a block within a database transaction on the IO dispatcher. */
suspend fun <T> query(block: () -> T): T = withContext(Dispatchers.IO) {
    transaction { block() }
}

// ============================================================================
//  AUTHENTICATION HELPERS (AttributeKey-based caching)
// ============================================================================

private val UserIdKey = AttributeKey<String>("UserId")
private val CurrentUserKey = AttributeKey<JwtTokenRequest>("CurrentUser")

fun ApplicationCall.currentUser(): JwtTokenRequest {
    return attributes.getOrNull(CurrentUserKey)
        ?: principal<JwtTokenRequest>()?.also { attributes.put(CurrentUserKey, it) }
        ?: throw IllegalStateException("No authenticated user found")
}

fun ApplicationCall.currentUserOrNull(): JwtTokenRequest? {
    return attributes.getOrNull(CurrentUserKey) ?: principal<JwtTokenRequest>()?.also {
        attributes.put(CurrentUserKey, it)
    }
}

val ApplicationCall.currentUserId: String
    get() = attributes.getOrNull(UserIdKey)
        ?: currentUser().userId.also { attributes.put(UserIdKey, it) }

// ============================================================================
//  ROLE / PERMISSION HELPERS
// ============================================================================

fun ApplicationCall.hasRole(role: UserType): Boolean =
    currentUserOrNull()?.hasRole(role) ?: false

fun ApplicationCall.hasAccessTo(role: UserType): Boolean =
    currentUserOrNull()?.hasAccessTo(role) ?: false

fun ApplicationCall.getCurrentUserType(): UserType? =
    currentUserOrNull()?.getUserType()

suspend fun ApplicationCall.requireRole(role: UserType, useHierarchy: Boolean = true): Boolean {
    val user = currentUserOrNull()
        ?: run { respond(HttpStatusCode.Unauthorized, "Unauthorized"); return false }
    val hasAccess = if (useHierarchy) user.hasAccessTo(role) else user.hasRole(role)
    if (!hasAccess) { respond(HttpStatusCode.Forbidden, "Forbidden"); return false }
    return true
}



// ============================================================================
//  REQUEST PARAMETER HELPERS
//  These throw MissingParameterException instead of responding directly,
//  allowing StatusPages to handle all errors centrally.
// ============================================================================

/**
 * Validates that all required parameters are present.
 * Throws MissingParameterException if any are missing (handled by StatusPages).
 * 
 * Usage: val (param1, param2) = call.requireParameters("param1", "param2")
 */
fun ApplicationCall.requireParameters(vararg requiredParams: String): List<String> {
    val missingParams = requiredParams.filterNot { parameters.contains(it) }
    if (missingParams.isNotEmpty()) {
        throw MissingParameterException(missingParams.first())
    }
    return requiredParams.map { parameters[it]!! }
}

/**
 * Extracts limit and offset parameters from the request.
 * Defaults to 10 and 0 respectively for products, 20 and 0 for others.
 */
fun ApplicationCall.paginationParameters(defaultLimit: Int = 20, defaultOffset: Int = 0): Pair<Int, Int> {
    val limit = parameters["limit"]?.toIntOrNull() ?: defaultLimit
    val offset = parameters["offset"]?.toIntOrNull() ?: defaultOffset
    return limit to offset
}

/**
 * @deprecated Use requireParameters() instead. This function bypasses StatusPages.
 */
@Deprecated(
    "Use requireParameters() instead. This function bypasses StatusPages.",
    ReplaceWith("requireParameters(*requiredParams)")
)
suspend fun ApplicationCall.requiredParameters(vararg requiredParams: String): List<String>? {
    val missingParams = requiredParams.filterNot { parameters.contains(it) }
    if (missingParams.isNotEmpty()) {
        respond(
            io.ktor.http.HttpStatusCode.BadRequest,
            com.piashcse.utils.ApiError("Missing parameters: ${missingParams.joinToString()}")
        )
        return null
    }
    return requiredParams.map { parameters[it]!! }
}
