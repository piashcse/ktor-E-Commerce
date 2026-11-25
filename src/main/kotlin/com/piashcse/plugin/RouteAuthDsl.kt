package com.piashcse.plugin

import com.piashcse.constants.AppConstants.Authentication.API_KEY_AUTHENTICATOR
import com.piashcse.constants.AppConstants.Authentication.JWT_AUTHENTICATOR
import com.piashcse.constants.Permission
import com.piashcse.constants.UserType
import com.piashcse.constants.defaultPermissions
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.utils.common.ApiError
import com.piashcse.utils.common.ApiErrorCode
import com.piashcse.utils.validator.ForbiddenException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.path
import io.ktor.server.response.*
import io.ktor.server.routing.*

private suspend fun ApplicationCall.respondAuthError(
    status: HttpStatusCode,
    message: String,
) {
    respond(
        status,
        ApiError.of(
            message = message,
            code = if (status == HttpStatusCode.Unauthorized) ApiErrorCode.UNAUTHORIZED else ApiErrorCode.FORBIDDEN,
            status = status,
            requestId = requestId(),
        ),
    )
}

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
                call.respondAuthError(HttpStatusCode.Unauthorized, "Missing or invalid token")
                return@on
            }

            if (allowedRoles.isEmpty()) {
                // A bare role gate means "any authenticated human", not
                // "any API key". There is no route permission to compare the
                // key's scopes against here, so fail closed.
                if (principal.isApiKey) {
                    call.respondAuthError(HttpStatusCode.Forbidden, "API keys cannot access this session route")
                }
                return@on
            }

            val exact = pluginConfig.exactMatch
            val hasAccess =
                allowedRoles.any { role ->
                    if (principal.isApiKey) {
                        // API keys are scope-bound, but must still satisfy exact-role
                        // gates (customerOnlyAuth must not accept a seller key).
                        val exactRoleMatches = !exact || principal.hasRole(role)
                        exactRoleMatches &&
                            role.defaultPermissions().all {
                                it.code in principal.scopes || it.name in principal.scopes
                            }
                    } else if (exact) {
                        principal.getUserType()?.hasExactRole(role) == true
                    } else {
                        principal.hasAccessTo(role)
                    }
                }
            if (!hasAccess) {
                call.respondAuthError(HttpStatusCode.Forbidden, "Permission Denied: Insufficient privileges")
                return@on
            }
        }
    }

class RoleAuthorizationConfig {
    var roles: List<UserType> = emptyList()

    /** When true, roles must match exactly (no hierarchy): sellers/admins cannot pass a CUSTOMER gate. */
    var exactMatch: Boolean = false
}

val PermissionAuthorizationPlugin =
    createRouteScopedPlugin(
        name = "PermissionAuthorizationPlugin",
        createConfiguration = ::PermissionAuthorizationConfig,
    ) {
        val required = pluginConfig.permissions
        on(AuthenticationChecked) { call ->
            if (call.response.isCommitted) return@on
            val principal = call.principal<JwtTokenRequest>()
            if (principal == null) {
                call.respondAuthError(HttpStatusCode.Unauthorized, "Missing or invalid token")
                return@on
            }
            if (required.isNotEmpty() && required.none { principal.hasPermission(it) }) {
                val message = "Permission Denied: missing ${required.joinToString { it.name }}"
                call.respondAuthError(HttpStatusCode.Forbidden, message)
            }
        }
    }

class PermissionAuthorizationConfig {
    var permissions: List<Permission> = emptyList()
}

/** Authenticates with JWT or API key, then requires at least one permission. */
fun Route.requirePermission(
    vararg permissions: Permission,
    build: Route.() -> Unit,
) {
    authenticate(JWT_AUTHENTICATOR, API_KEY_AUTHENTICATOR) {
        val routeWithAuth =
            createChild(
                object : RouteSelector() {
                    override suspend fun evaluate(
                        context: RoutingResolveContext,
                        segmentIndex: Int,
                    ) = RouteSelectorEvaluation.Constant
                },
            )
        routeWithAuth.install(PermissionAuthorizationPlugin) {
            this.permissions = permissions.toList()
        }
        routeWithAuth.build()
    }
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
    authenticate(JWT_AUTHENTICATOR, API_KEY_AUTHENTICATOR) {
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

/**
 * Coarse scope for a seller API key. Seller accounts still have all role
 * defaults; a scoped key is restricted to the matching subset.
 */
private fun sellerScopeFor(path: String): Permission? =
    when (path.removePrefix("/api/v1/seller/").substringBefore('/')) {
        "shops", "members", "kyc", "commission" -> Permission.SHOP_MANAGE
        "products", "inventories", "warehouses" -> Permission.INVENTORY_MANAGE
        "orders", "pickup-orders", "pos", "b2b" -> Permission.ORDER_MANAGE
        "refund-requests", "rma-requests" -> Permission.REFUND_MANAGE
        "payouts" -> Permission.PAYOUT_MANAGE
        "dashboard", "readiness" -> Permission.DASHBOARD_VIEW
        "notifications" -> Permission.NOTIFICATION_SEND
        "coupons", "promotions", "shipping-methods", "reviews" -> Permission.ORDER_MANAGE
        else -> null
    }

/**
 * Seller routes are available to seller accounts and to accepted shop staff.
 * The latter are CUSTOMER accounts with an active shop_member row; their
 * services resolve the inviting shop through findSellerByUserId().
 */
val ShopMemberAuthorizationPlugin =
    createRouteScopedPlugin(name = "ShopMemberAuthorizationPlugin") {
        on(AuthenticationChecked) { call ->
            if (call.response.isCommitted) return@on
            val principal = call.principal<JwtTokenRequest>()
            if (principal == null) {
                call.respondAuthError(HttpStatusCode.Unauthorized, "Missing or invalid token")
                return@on
            }
            val membershipKey = "shopmember:${principal.userId}"
            val cachedMembership = com.piashcse.service.CacheService.cache.get<Boolean>(membershipKey)
            val isShopMember =
                cachedMembership ?: runCatching {
                    com.piashcse.utils.extension.query {
                        com.piashcse.utils.extension.findSellerByUserId(principal.userId) != null
                    }
                }.getOrDefault(false).also { isMember ->
                    com.piashcse.service.CacheService.cache.set(membershipKey, isMember, 30)
                }
            val allowed = principal.hasRole(UserType.SELLER) || isShopMember
            val scopeOk =
                !principal.isApiKey || sellerScopeFor(call.request.path())?.let { principal.hasPermission(it) } != false
            if (!allowed || !scopeOk) {
                call.respondAuthError(HttpStatusCode.Forbidden, "Permission Denied: seller access required")
            }
        }
    }

fun Route.customerAuth(build: Route.() -> Unit) = requireRole(UserType.CUSTOMER, build = build)

/** Strict customer-only scope: CUSTOMER role exactly — sellers/admins are denied. */
fun Route.customerOnlyAuth(build: Route.() -> Unit) = requireRoleExact(UserType.CUSTOMER, build = build)

fun Route.sellerAuth(build: Route.() -> Unit) {
    authenticate(JWT_AUTHENTICATOR, API_KEY_AUTHENTICATOR) {
        val routeWithAuth =
            createChild(
                object : RouteSelector() {
                    override suspend fun evaluate(
                        context: RoutingResolveContext,
                        segmentIndex: Int,
                    ) = RouteSelectorEvaluation.Constant
                },
            )
        routeWithAuth.install(ShopMemberAuthorizationPlugin)
        routeWithAuth.build()
    }
}

fun Route.adminAuth(build: Route.() -> Unit) = requireRole(UserType.ADMIN, UserType.SUPER_ADMIN, build = build)

/**
 * Mixed-role routes (customer/seller self-service plus admin support) must not
 * let a narrowly-scoped admin bypass the permission gate. Call this at the top
 * of a handler that also accepts admins.
 */
fun ApplicationCall.requireAdminPermission(permission: Permission) {
    val principal = principal<JwtTokenRequest>() ?: return
    if (principal.getUserType()?.isAdminOrHigher == true && !principal.hasPermission(permission)) {
        throw ForbiddenException("Permission Denied: missing ${permission.code}")
    }
}

/**
 * Credential/session/PII mutations must be performed by a human JWT session,
 * never by a scoped API key. Bare `requireRole { }` routes otherwise accept any
 * key without checking its scopes.
 */
fun ApplicationCall.requireJwtSession() {
    if (principal<JwtTokenRequest>()?.isApiKey == true) {
        throw ForbiddenException("API keys cannot perform this session operation")
    }
}

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
