package com.piashcse.feature.user

import com.piashcse.constants.KycStatus
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
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.readQuery
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.NotFoundException
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

/** Admin user directory queries and lockout maintenance. */
class AdminUserService {
    suspend fun search(
        limit: Int,
        offset: Int,
        search: String?,
        userType: UserType?,
        active: Boolean?,
        verified: Boolean?,
        guest: Boolean?,
    ): PaginatedResponse<AdminUserSummaryResponse> =
        readQuery {
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

    suspend fun detail(id: String): AdminUserDetailResponse =
        query {
            val user = UserDAO.findById(id) ?: throw NotFoundException("User not found")
            val seller = SellerDAO.find { SellerTable.userId eq user.id }.firstOrNull()
            val shop = seller?.shopId?.let { ShopDAO.findById(it.value) }
            AdminUserDetailResponse(
                user = user.toSummary(),
                sellerId = seller?.id?.value,
                shopId = shop?.id?.value,
                shopStatus = shop?.status,
                kycStatus = seller?.kycStatus,
                orderCount = OrderTable.selectAll().where { OrderTable.userId eq user.id }.count(),
            )
        }

    suspend fun loginAttempts(id: String): List<LoginAttemptResponse> =
        query {
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
        }

    suspend fun clearLockouts(id: String) {
        query {
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
    }

    private fun UserDAO.toSummary() =
        AdminUserSummaryResponse(
            id = id.value,
            email = email,
            userType = userType,
            isVerified = isVerified,
            isActive = isActive,
            mfaEnabled = mfaEnabled,
            isGuest = isGuest,
            createdAt = createdAt.toUtcString(),
        )
}
