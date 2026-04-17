package com.piashcse.plugin

import com.piashcse.constants.UserType
import com.piashcse.model.request.JwtTokenRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Architectural DSL for Role-Based Routing.
 * Promotes the Single Authenticator pattern (validating JWT once),
 * followed by Role-Based Access Control (RBAC).
 */

val RoleAuthorizationPlugin = createRouteScopedPlugin(
    name = "RoleAuthorizationPlugin",
    createConfiguration = ::RoleAuthorizationConfig
) {
    val allowedRoles = pluginConfig.roles

    onCall { call ->
        val principal = call.principal<JwtTokenRequest>()
        if (principal == null) {
            call.respond(HttpStatusCode.Unauthorized, "Missing or invalid token")
            return@onCall
        }

        // If no specific roles required, any valid JWT passes
        if (allowedRoles.isEmpty()) return@onCall

        val hasAccess = allowedRoles.any { role -> principal.hasAccessTo(role) }
        if (!hasAccess) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Permission Denied: Insufficient privileges"))
        }
    }
}

class RoleAuthorizationConfig {
    var roles: Array<out UserType> = emptyArray()
}

/**
 * Explicitly restrict routes to specific UserTypes with hierarchy.
 * Inherently allows ANY valid token if parameters are empty.
 */
fun Route.requireRole(vararg roles: UserType, build: Route.() -> Unit) {
    // 1. Authenticate the JWT globally (only ONE provider executes)
    authenticate("jwt-auth") {
        // 2. Perform the fast RBAC interceptor check
        val routeWithAuth = createChild(object : RouteSelector() {
            override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int) = RouteSelectorEvaluation.Constant
        })
        routeWithAuth.install(RoleAuthorizationPlugin) {
            this.roles = roles
        }
        routeWithAuth.build()
    }
}

// Convenience Scope Functions for drastically cleaner routing semantics
fun Route.customerAuth(build: Route.() -> Unit) = requireRole(UserType.CUSTOMER, build = build)
fun Route.sellerAuth(build: Route.() -> Unit) = requireRole(UserType.SELLER, build = build)
fun Route.adminAuth(build: Route.() -> Unit) = requireRole(UserType.ADMIN, UserType.SUPER_ADMIN, build = build)
fun Route.superAdminAuth(build: Route.() -> Unit) = requireRole(UserType.SUPER_ADMIN, build = build)
