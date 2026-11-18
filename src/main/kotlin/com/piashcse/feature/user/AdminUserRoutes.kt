@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

@file:Suppress("MatchingDeclarationName")

package com.piashcse.feature.user

import com.piashcse.constants.KycStatus
import com.piashcse.constants.Permission
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.LoginAttemptDAO
import com.piashcse.database.entities.LoginAttemptTable
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.OtpAttemptDAO
import com.piashcse.database.entities.OtpAttemptTable
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.feature.auth.AuthRepository
import com.piashcse.plugin.generalRateLimit
import com.piashcse.plugin.requirePermission
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.queryBooleanOrNull
import com.piashcse.utils.extension.respondMessage
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.ktor.ext.inject

@Serializable
data class AdminUserSummaryResponse(
    val id: String,
    val email: String,
    val userType: UserType,
    val isVerified: Boolean,
    val isActive: Boolean,
    val mfaEnabled: Boolean,
    val isGuest: Boolean,
    val createdAt: String?,
)

@Serializable
data class AdminUserDetailResponse(
    val user: AdminUserSummaryResponse,
    val sellerId: String? = null,
    val shopId: String? = null,
    val shopStatus: ShopStatus? = null,
    val kycStatus: KycStatus? = null,
    val orderCount: Long = 0,
)

@Serializable
data class AdminUserTypeRequest(val userType: UserType)

@Serializable
data class LoginAttemptResponse(
    val id: String,
    val email: String,
    val userType: UserType,
    val ipAddress: String?,
    val attemptCount: Int,
    val lockedUntil: String?,
    val isLocked: Boolean,
)

/** Admin user directory so operators can resolve IDs shown by other consoles. */
fun Route.adminUserRoutes() {
    requirePermission(Permission.USER_MANAGE) {
        generalRateLimit {
            get {
                val (limit, offset) = call.pageRequest()
                val search = call.request.queryParameters["query"]?.trim()?.lowercase()
                val userType = call.request.queryParameters["userType"]?.parseEnum<UserType>("userType")
                val active = call.queryBooleanOrNull("active")
                val verified = call.queryBooleanOrNull("verified")
                val guest = call.queryBooleanOrNull("guest")

                val result =
                    transaction {
                        UserTable.selectAll()
                            .also { query ->
                                if (!search.isNullOrBlank()) {
                                    val pattern = "%$search%"
                                    query.andWhere { UserTable.email.lowerCase() like pattern }
                                }
                                userType?.let { query.andWhere { UserTable.userType eq it } }
                                active?.let { query.andWhere { UserTable.isActive eq it } }
                                verified?.let { query.andWhere { UserTable.isVerified eq it } }
                                guest?.let { query.andWhere { UserTable.isGuest eq it } }
                                query.orderBy(UserTable.createdAt to SortOrder.DESC)
                            }.toPaginatedResponse(limit, offset) { row ->
                                AdminUserSummaryResponse(
                                    id = row[UserTable.id].value,
                                    email = row[UserTable.email],
                                    userType = row[UserTable.userType],
                                    isVerified = row[UserTable.isVerified],
                                    isActive = row[UserTable.isActive],
                                    mfaEnabled = row[UserTable.mfaEnabled],
                                    isGuest = row[UserTable.isGuest],
                                    createdAt = row[UserTable.createdAt].toUtcString(),
                                )
                            }
                    }
                call.respondOk(result)
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
    get("{id}") {
        val id = call.requirePathParameter("id")
        call.respondOk(
            transaction {
                val user = UserDAO.findById(id) ?: throw NotFoundException("User not found")
                val seller = SellerDAO.find { SellerTable.userId eq user.id }.firstOrNull()
                val shop = seller?.shopId?.let { ShopDAO.findById(it.value) }
                AdminUserDetailResponse(
                    user =
                        AdminUserSummaryResponse(
                            id = user.id.value,
                            email = user.email,
                            userType = user.userType,
                            isVerified = user.isVerified,
                            isActive = user.isActive,
                            mfaEnabled = user.mfaEnabled,
                            isGuest = user.isGuest,
                            createdAt = user.createdAt.toUtcString(),
                        ),
                    sellerId = seller?.id?.value,
                    shopId = shop?.id?.value,
                    shopStatus = shop?.status,
                    kycStatus = seller?.kycStatus,
                    orderCount = OrderTable.selectAll().where { OrderTable.userId eq user.id }.count(),
                )
            },
        )
    }.describe {
        summary = "Admin: user detail"
        description = "Account state plus seller/shop/KYC and order count"
    }

    get("{id}/login-attempts") {
        val id = call.requirePathParameter("id")
        call.respondOk(
            transaction {
                val user = UserDAO.findById(id) ?: throw NotFoundException("User not found")
                LoginAttemptDAO.find { LoginAttemptTable.email eq user.email }
                    .map {
                        LoginAttemptResponse(
                            id = it.id.value,
                            email = it.email,
                            userType = it.userType,
                            ipAddress = it.ipAddress,
                            attemptCount = it.attemptCount,
                            lockedUntil = it.lockedUntil?.toString(),
                            isLocked = it.isLocked,
                        )
                    }
            },
        )
    }.describe { summary = "Admin: inspect login/OTP lockouts" }

    post("{id}/unlock") {
        val id = call.requirePathParameter("id")
        transaction {
            val user = UserDAO.findById(id) ?: throw NotFoundException("User not found")
            LoginAttemptDAO.find { LoginAttemptTable.email eq user.email }.forEach {
                it.attemptCount = 0
                it.lockedUntil = null
            }
            OtpAttemptDAO.find { OtpAttemptTable.userId eq user.id }.forEach {
                it.attemptCount = 0
                it.lockedUntil = null
            }
        }
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
