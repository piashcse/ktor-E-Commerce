package com.piashcse.utils.extension

import com.piashcse.constants.UserType
import com.piashcse.model.request.JwtTokenRequest
import io.ktor.server.application.*
import io.ktor.server.auth.*
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

fun ApplicationCall.getCurrentUserType(): UserType? = currentUserOrNull()?.getUserType()
