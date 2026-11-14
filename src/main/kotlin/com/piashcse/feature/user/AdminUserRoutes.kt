@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.user

import com.piashcse.constants.Permission
import com.piashcse.constants.UserType
import com.piashcse.database.entities.UserTable
import com.piashcse.plugin.generalRateLimit
import com.piashcse.plugin.requirePermission
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.extension.toPaginatedResponse
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

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

/** Admin user directory so operators can resolve IDs shown by other consoles. */
fun Route.adminUserRoutes() {
    requirePermission(Permission.USER_MANAGE) {
        generalRateLimit {
            get {
                val (limit, offset) = call.paginateQueryParams()
                val search = call.request.queryParameters["query"]?.trim()?.lowercase()
                val userType =
                    call.request.queryParameters["userType"]?.parseEnum<UserType>("userType")

                UserTable.selectAll()
                    .also { query ->
                        if (!search.isNullOrBlank()) {
                            val pattern = "%$search%"
                            query.andWhere { UserTable.email.lowerCase() like pattern }
                        }
                        userType?.let { query.andWhere { UserTable.userType eq it } }
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
                    }.let { call.respondOk(it) }
            }.describe {
                summary = "Admin: search users"
                description = "Filter by email substring and user type; returns account state"
            }
        }
    }
}
