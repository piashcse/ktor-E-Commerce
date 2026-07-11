package com.piashcse.plugin

import com.piashcse.constants.AppConstants.Authentication.JWT_AUTHENTICATOR
import com.piashcse.constants.UserType
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.utils.common.ApiError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val RoleAuthorizationPlugin =
    createRouteScopedPlugin(
        name = "RoleAuthorizationPlugin",
        createConfiguration = ::RoleAuthorizationConfig,
    ) {
        val allowedRoles = pluginConfig.roles

        onCall { call ->
            if (call.response.isCommitted) return@onCall

            val principal = call.principal<JwtTokenRequest>()
            if (principal == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Missing or invalid token"))
                return@onCall
            }

            if (allowedRoles.isEmpty()) return@onCall

            val hasAccess = allowedRoles.any { role -> principal.hasAccessTo(role) }
            if (!hasAccess) {
                call.respond(HttpStatusCode.Forbidden, ApiError("Permission Denied: Insufficient privileges"))
                return@onCall
            }
        }
    }

class RoleAuthorizationConfig {
    var roles: List<UserType> = emptyList()
}

fun Route.requireRole(
    vararg roles: UserType,
    build: Route.() -> Unit,
) {
    authenticate(JWT_AUTHENTICATOR) {
        val routeWithAuth =
            createChild(
                object : RouteSelector() {
                    override suspend fun evaluate(
                        context: RoutingResolveContext,
                        segmentIndex: Int,
                    ) = RouteSelectorEvaluation.Constant
                },
            )
        routeWithAuth.install(RoleAuthorizationPlugin) {
            this.roles = roles.toList()
        }
        routeWithAuth.build()
    }
}

// Convenience Scope Functions for drastically cleaner routing semantics
fun Route.customerAuth(build: Route.() -> Unit) = requireRole(UserType.CUSTOMER, build = build)

fun Route.sellerAuth(build: Route.() -> Unit) = requireRole(UserType.SELLER, build = build)

fun Route.adminAuth(build: Route.() -> Unit) = requireRole(UserType.ADMIN, UserType.SUPER_ADMIN, build = build)

fun Route.superAdminAuth(build: Route.() -> Unit) = requireRole(UserType.SUPER_ADMIN, build = build)

// Rate Limit DSL helpers
fun Route.writeRateLimit(build: Route.() -> Unit) {
    rateLimit(RateLimitName(RateLimitNames.WRITE)) { build() }
}

fun Route.sellerWriteRateLimit(build: Route.() -> Unit) {
    rateLimit(RateLimitName(RateLimitNames.SELLER_WRITE)) { build() }
}

fun Route.adminWriteRateLimit(build: Route.() -> Unit) {
    rateLimit(RateLimitName(RateLimitNames.ADMIN_WRITE)) { build() }
}
