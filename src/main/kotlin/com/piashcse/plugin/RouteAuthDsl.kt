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

        // MUST run on AuthenticationChecked: route onCall fires before auth
        // providers populate the principal (principal is always null there).
        on(AuthenticationChecked) { call ->
            if (call.response.isCommitted) return@on

            val principal = call.principal<JwtTokenRequest>()
            if (principal == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Missing or invalid token", code = "UNAUTHORIZED"))
                return@on
            }

            if (allowedRoles.isEmpty()) return@on

            val exact = pluginConfig.exactMatch
            val hasAccess =
                allowedRoles.any { role ->
                    if (exact) {
                        principal.getUserType()?.hasExactRole(role) == true
                    } else {
                        principal.hasAccessTo(role)
                    }
                }
            if (!hasAccess) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiError("Permission Denied: Insufficient privileges", code = "FORBIDDEN"),
                )
                return@on
            }
        }
    }

class RoleAuthorizationConfig {
    var roles: List<UserType> = emptyList()

    /** When true, roles must match exactly (no hierarchy): sellers/admins cannot pass a CUSTOMER gate. */
    var exactMatch: Boolean = false
}

fun Route.requireRole(
    vararg roles: UserType,
    build: Route.() -> Unit,
) {
    authorized(*roles, exactMatch = false, build = build)
}

/**
 * Strict least-privilege gate: exact role match only.
 * customerOnlyAuth blocks sellers/admins from customer self-service routes
 * (customerAuth is hierarchical — every role passes a CUSTOMER check).
 */
fun Route.requireRoleExact(
    vararg roles: UserType,
    build: Route.() -> Unit,
) {
    authorized(*roles, exactMatch = true, build = build)
}

private fun Route.authorized(
    vararg roles: UserType,
    exactMatch: Boolean,
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
            this.exactMatch = exactMatch
        }
        routeWithAuth.build()
    }
}

// Convenience Scope Functions for drastically cleaner routing semantics
fun Route.customerAuth(build: Route.() -> Unit) = requireRole(UserType.CUSTOMER, build = build)

/** Strict customer-only scope: CUSTOMER role exactly — sellers/admins are denied. */
fun Route.customerOnlyAuth(build: Route.() -> Unit) = requireRoleExact(UserType.CUSTOMER, build = build)

fun Route.sellerAuth(build: Route.() -> Unit) = requireRole(UserType.SELLER, build = build)

fun Route.adminAuth(build: Route.() -> Unit) = requireRole(UserType.ADMIN, UserType.SUPER_ADMIN, build = build)

fun Route.superAdminAuth(build: Route.() -> Unit) = requireRole(UserType.SUPER_ADMIN, build = build)

// Rate Limit DSL helpers
fun Route.writeRateLimit(build: Route.() -> Unit) {
    rateLimit(RateLimitName(RateLimitNames.WRITE)) { build() }
}

fun Route.generalRateLimit(build: Route.() -> Unit) {
    rateLimit(RateLimitName(RateLimitNames.GENERAL)) { build() }
}

fun Route.searchRateLimit(build: Route.() -> Unit) {
    rateLimit(RateLimitName(RateLimitNames.SEARCH)) { build() }
}

fun Route.authRateLimit(build: Route.() -> Unit) {
    rateLimit(RateLimitName(RateLimitNames.AUTH)) { build() }
}

fun Route.otpRateLimit(build: Route.() -> Unit) {
    rateLimit(RateLimitName(RateLimitNames.OTP)) { build() }
}

fun Route.refreshTokenRateLimit(build: Route.() -> Unit) {
    rateLimit(RateLimitName(RateLimitNames.REFRESH_TOKEN)) { build() }
}

fun Route.sellerWriteRateLimit(build: Route.() -> Unit) {
    rateLimit(RateLimitName(RateLimitNames.SELLER_WRITE)) { build() }
}

fun Route.adminWriteRateLimit(build: Route.() -> Unit) {
    rateLimit(RateLimitName(RateLimitNames.ADMIN_WRITE)) { build() }
}

/**
 * Combined auth + rate-limit scope: folds requireRole + rateLimit into one nest.
 * Identical semantics to `requireRole(*roles) { rateLimit(limitName) { build() } }`.
 */
fun Route.authenticatedWrite(
    vararg roles: UserType,
    limitName: RateLimitName,
    build: Route.() -> Unit,
) {
    requireRole(*roles) {
        rateLimit(limitName) { build() }
    }
}
