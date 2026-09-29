package com.piashcse.utils.extension

import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.utils.validator.UnauthorizedException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.*

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
    get() =
        attributes.getOrNull(UserIdKey)
            ?: currentUser().userId.also { attributes.put(UserIdKey, it) }

// ============================================================================
//  ROLE / PERMISSION HELPERS
// ============================================================================

fun ApplicationCall.hasRole(role: UserType): Boolean = currentUserOrNull()?.hasRole(role) ?: false

fun ApplicationCall.hasAccessTo(role: UserType): Boolean = currentUserOrNull()?.hasAccessTo(role) ?: false

fun ApplicationCall.getCurrentUserType(): UserType? = currentUserOrNull()?.getUserType()

/**
 * Returns (userId, email, userType-string) for audit/status calls.
 * Same exceptions as manual currentUserId/currentUserOrNull extraction.
 */
fun ApplicationCall.actor(): Triple<String, String?, String?> =
    Triple(currentUserId, currentUserOrNull()?.email, currentUserOrNull()?.userType)

/**
 * Returns the caller's UserType or throws 401 — replaces the
 * copy-pasted getCurrentUserType() ?: throw UnauthorizedException triple.
 */
fun ApplicationCall.requireUserType(): UserType = getCurrentUserType() ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED)

suspend fun ApplicationCall.requireRole(
    role: UserType,
    useHierarchy: Boolean = true,
): Boolean {
    val user =
        currentUserOrNull()
            ?: run {
                respond(HttpStatusCode.Unauthorized, "Unauthorized")
                return false
            }
    val hasAccess = if (useHierarchy) user.hasAccessTo(role) else user.hasRole(role)
    if (!hasAccess) {
        respond(HttpStatusCode.Forbidden, "Forbidden")
        return false
    }
    return true
}
