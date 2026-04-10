package com.piashcse.utils.extension

import com.piashcse.constants.UserType
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.CommonException
import com.piashcse.utils.Permission
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

// Attribute keys for caching - userId cached directly for maximum performance
private val UserIdKey = AttributeKey<String>("UserId")
private val CurrentUserKey = AttributeKey<JwtTokenRequest>("CurrentUser")

fun String.notFoundException(): CommonException {
    return CommonException("$this is not Exist")
}

fun String.alreadyExistException(secondaryInfo: String = ""): CommonException {
    return if (secondaryInfo.isEmpty()) CommonException("$this is already Exist")
    else CommonException("$this $secondaryInfo is already Exist")
}

suspend fun <T> query(block: () -> T): T = withContext(Dispatchers.IO) {
    transaction {
        block()
    }
}

/**
 * Get the current authenticated user with caching.
 * Caches the user object to avoid repeated JWT principal lookups.
 */
fun ApplicationCall.currentUser(): JwtTokenRequest {
    return attributes.getOrNull(CurrentUserKey)
        ?: principal<JwtTokenRequest>()?.also { attributes.put(CurrentUserKey, it) }
        ?: throw IllegalStateException("No authenticated user found")
}

/**
 * Get the current user or null if not authenticated.
 * Uses caching to avoid repeated JWT principal lookups.
 */
fun ApplicationCall.currentUserOrNull(): JwtTokenRequest? {
    return attributes.getOrNull(CurrentUserKey) ?: principal<JwtTokenRequest>()?.also {
        attributes.put(CurrentUserKey, it)
    }
}

/**
 * Get the current authenticated user's ID with direct caching.
 * This is the most efficient way to get userId - caches the String directly
 * to avoid both JWT parsing and property access overhead.
 */
val ApplicationCall.currentUserId: String
    get() = attributes.getOrNull(UserIdKey)
        ?: currentUser().userId.also { attributes.put(UserIdKey, it) }

/**
 * Check if current user has a specific role.
 * Uses cached user to avoid repeated JWT principal lookups.
 */
fun ApplicationCall.hasRole(role: UserType): Boolean {
    return currentUserOrNull()?.hasRole(role) ?: false
}

/**
 * Check if current user has access to specific role (with hierarchy).
 * Uses cached user to avoid repeated JWT principal lookups.
 */
fun ApplicationCall.hasAccessTo(role: UserType): Boolean {
    return currentUserOrNull()?.hasAccessTo(role) ?: false
}

/**
 * Get current user's role.
 * Uses cached user to avoid repeated JWT principal lookups.
 */
fun ApplicationCall.getCurrentUserType(): UserType? {
    return currentUserOrNull()?.getUserType()
}

/**
 * Require specific role with optional hierarchy check.
 * Uses cached user and responds with appropriate HTTP status codes.
 */
suspend fun ApplicationCall.requireRole(role: UserType, useHierarchy: Boolean = true): Boolean {
    val user = currentUserOrNull()
    if (user == null) {
        respond(HttpStatusCode.Unauthorized, "Unauthorized")
        return false
    }

    val hasAccess = if (useHierarchy) user.hasAccessTo(role) else user.hasRole(role)
    if (!hasAccess) {
        respond(HttpStatusCode.Forbidden, "Forbidden: Insufficient permissions")
        return false
    }
    return true
}

/**
 * Check if current user has a specific permission.
 * Uses cached user to avoid repeated JWT principal lookups.
 */
fun ApplicationCall.hasPermission(permission: Permission): Boolean {
    return currentUserOrNull()?.hasPermission(permission) ?: false
}

/**
 * Require specific permission.
 * Uses cached user and responds with appropriate HTTP status codes.
 */
suspend fun ApplicationCall.requirePermission(permission: Permission): Boolean {
    val user = currentUserOrNull()
    if (user == null) {
        respond(HttpStatusCode.Unauthorized, "Unauthorized")
        return false
    }

    if (!user.hasPermission(permission)) {
        respond(HttpStatusCode.Forbidden, "Forbidden: Insufficient permissions")
        return false
    }
    return true
}

/**
 * Validate and retrieve required query parameters.
 * Returns null and sends BadRequest response if any parameters are missing.
 */
suspend fun ApplicationCall.requiredParameters(vararg requiredParams: String): List<String>? {
    val missingParams = requiredParams.filterNot { parameters.contains(it) }
    if (missingParams.isNotEmpty()) {
        respond(ApiResponse.success("Missing parameters: $missingParams", HttpStatusCode.BadRequest))
        return null
    }
    return requiredParams.map { parameters[it]!! }
}