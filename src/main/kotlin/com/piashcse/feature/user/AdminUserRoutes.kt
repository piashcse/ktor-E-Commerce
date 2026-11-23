@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

@file:Suppress("MatchingDeclarationName")

package com.piashcse.feature.user

import com.piashcse.constants.Permission
import com.piashcse.constants.UserType
import com.piashcse.feature.auth.AuthRepository
import com.piashcse.plugin.generalRateLimit
import com.piashcse.plugin.requirePermission
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.queryBooleanOrNull
import com.piashcse.utils.extension.respondMessage
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Admin user directory so operators can resolve IDs shown by other consoles. */
fun Route.adminUserRoutes() {
    val adminUserService: AdminUserService by inject()
    requirePermission(Permission.USER_MANAGE) {
        generalRateLimit {
            get {
                val (limit, offset) = call.pageRequest()
                val search = call.request.queryParameters["query"]?.trim()?.lowercase()
                val userType = call.request.queryParameters["userType"]?.parseEnum<UserType>("userType")
                val active = call.queryBooleanOrNull("active")
                val verified = call.queryBooleanOrNull("verified")
                val guest = call.queryBooleanOrNull("guest")
                call.respondOk(
                    adminUserService.search(
                        limit = limit,
                        offset = offset,
                        search = search,
                        userType = userType,
                        active = active,
                        verified = verified,
                        guest = guest,
                    ),
                )
            }.describe {
                summary = "Admin: search users"
                description = "Filter by email, user type and account state"
            }

            adminUserDetailRoutes()
        }
    }
}

private fun Route.adminUserDetailRoutes() {
    val authRepo: AuthRepository by inject()
    val adminUserService: AdminUserService by inject()

    get("{id}") {
        val id = call.requirePathParameter("id")
        call.respondOk(adminUserService.detail(id))
    }.describe {
        summary = "Admin: user detail"
        description = "Account state plus seller/shop/KYC and order count"
    }

    get("{id}/login-attempts") {
        val id = call.requirePathParameter("id")
        call.respondOk(adminUserService.loginAttempts(id))
    }.describe { summary = "Admin: inspect login/OTP lockouts" }

    post("{id}/unlock") {
        val id = call.requirePathParameter("id")
        adminUserService.clearLockouts(id)
        call.respondMessage("Account lockouts cleared")
    }.describe { summary = "Admin: clear login and OTP lockouts" }

    put("{id}/type") {
        val id = call.requirePathParameter("id")
        val body = call.receive<AdminUserTypeRequest>()
        if (!authRepo.changeUserType(call.currentUserId, id, body.userType)) {
            throw NotFoundException("User not found")
        }
        call.respondMessage("User type updated")
    }.describe { summary = "Admin: change a user's role" }

    put("{id}/deactivate") {
        val id = call.requirePathParameter("id")
        if (!authRepo.deactivateUser(call.currentUserId, id)) throw NotFoundException("User not found")
        call.respondMessage("User deactivated")
    }.describe { summary = "Admin: deactivate a user" }

    put("{id}/activate") {
        val id = call.requirePathParameter("id")
        if (!authRepo.activateUser(call.currentUserId, id)) throw NotFoundException("User not found")
        call.respondMessage("User activated")
    }.describe { summary = "Admin: activate a user" }
}
