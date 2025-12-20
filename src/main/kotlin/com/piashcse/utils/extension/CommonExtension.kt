package com.piashcse.utils.extension

import com.piashcse.constants.UserType
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.CommonException
import com.piashcse.utils.Permission
import com.piashcse.utils.Response
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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

fun ApplicationCall.currentUser(): JwtTokenRequest {
    return this.principal<JwtTokenRequest>() ?: throw IllegalStateException("No authenticated user found")
}

/**
 * Get the current user with optional role validation
 */
fun ApplicationCall.currentUserOrNull(): JwtTokenRequest? {
    return this.principal<JwtTokenRequest>()
}

/**
 * Check if current user has a specific role
 */
fun ApplicationCall.hasRole(role: UserType): Boolean {
    val user = this.principal<JwtTokenRequest>()
    return user?.hasRole(role) ?: false
}

/**
 * Check if current user has access to specific role (with hierarchy)
 */
fun ApplicationCall.hasAccessTo(role: UserType): Boolean {
    val user = this.principal<JwtTokenRequest>()
    return user?.hasAccessTo(role) ?: false
}

/**
 * Get current user's role
 */
fun ApplicationCall.getCurrentUserType(): UserType? {
    val user = this.principal<JwtTokenRequest>()
    return user?.getUserType()
}

/**
 * Require specific role with optional hierarchy check
 */
suspend fun ApplicationCall.requireRole(role: UserType, useHierarchy: Boolean = true): Boolean {
    val user = this.principal<JwtTokenRequest>()
    if (user == null) {
        this.respond(HttpStatusCode.Unauthorized, "Unauthorized")
        return false
    }

    val hasAccess = if (useHierarchy) user.hasAccessTo(role) else user.hasRole(role)
    if (!hasAccess) {
        this.respond(HttpStatusCode.Forbidden, "Forbidden: Insufficient permissions")
        return false
    }
    return true
}

/**
 * Check if current user has a specific permission
 */
fun ApplicationCall.hasPermission(permission: Permission): Boolean {
    val user = this.principal<JwtTokenRequest>()
    return com.piashcse.utils.Permissions.hasPermission(user, permission)
}

/**
 * Require specific permission
 */
suspend fun ApplicationCall.requirePermission(permission: Permission): Boolean {
    val user = this.principal<JwtTokenRequest>()
    if (user == null) {
        this.respond(HttpStatusCode.Unauthorized, "Unauthorized")
        return false
    }

    if (!user.hasPermission(permission)) {
        this.respond(HttpStatusCode.Forbidden, "Forbidden: Insufficient permissions")
        return false
    }
    return true
}

suspend fun ApplicationCall.requiredParameters(vararg requiredParams: String): List<String>? {
    val missingParams = requiredParams.filterNot { this.parameters.contains(it) }
    if (missingParams.isNotEmpty()) {
        this.respond(ApiResponse.success("Missing parameters: $missingParams", HttpStatusCode.BadRequest))
        return null
    }
    return requiredParams.map { this.parameters[it]!! }
}