package com.piashcse.feature.auth

import com.piashcse.constants.UserType
import com.piashcse.database.entities.LoginAttemptDAO
import com.piashcse.database.entities.LoginAttemptTable
import com.piashcse.utils.common.AppClock
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull

/** Per-email/role/IP login lockout tracking. */
class LoginAttemptService {
    private fun loginAttemptPredicate(
        email: String,
        userType: UserType,
        ipAddress: String? = null,
    ) = (LoginAttemptTable.email eq normalizeEmail(email)) and
        (LoginAttemptTable.userType eq userType) and
        if (ipAddress == null) {
            LoginAttemptTable.ipAddress.isNull()
        } else {
            LoginAttemptTable.ipAddress eq ipAddress
        }

    suspend fun recordFailedAttempt(
        email: String,
        userType: UserType,
        ipAddress: String?,
    ): Int =
        query {
            val existing =
                LoginAttemptDAO.find { loginAttemptPredicate(email, userType, ipAddress) }
                    .forUpdate()
                    .singleOrNull()
            if (existing != null) {
                existing.attemptCount++
                existing.ipAddress = ipAddress
                existing.attemptCount
            } else {
                LoginAttemptDAO.new {
                    this.email = normalizeEmail(email)
                    this.userType = userType
                    this.ipAddress = ipAddress
                    this.attemptCount = 1
                }
                1
            }
        }

    suspend fun resetLoginAttempts(
        email: String,
        userType: UserType,
        ipAddress: String?,
    ) = query {
        LoginAttemptDAO.find { loginAttemptPredicate(email, userType, ipAddress) }.singleOrNull()?.apply {
            attemptCount = 0
            lockedUntil = null
        }
        Unit
    }

    suspend fun getLoginAttempt(
        email: String,
        userType: UserType,
        ipAddress: String?,
    ): LoginAttemptDAO? =
        query {
            LoginAttemptDAO.find { loginAttemptPredicate(email, userType, ipAddress) }.singleOrNull()
        }

    suspend fun lockAccount(
        email: String,
        userType: UserType,
        lockDurationMinutes: Long,
        ipAddress: String?,
    ): Boolean =
        query {
            LoginAttemptDAO.find { loginAttemptPredicate(email, userType, ipAddress) }.singleOrNull()
                ?.apply { lockedUntil = AppClock.now().plusSeconds(lockDurationMinutes * 60) } != null
        }

    private fun normalizeEmail(value: String): String = value.trim().lowercase()
}
