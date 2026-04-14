package com.piashcse.feature.auth

import com.piashcse.constants.UserType
import com.piashcse.database.entities.LoginAttemptDAO
import com.piashcse.database.entities.LoginAttemptTable
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

interface LoginAttemptRepository {
    suspend fun getAttempt(email: String, userType: UserType): LoginAttemptDAO?
    suspend fun recordFailedAttempt(email: String, userType: UserType, ipAddress: String?): Int
    suspend fun resetAttempts(email: String, userType: UserType): Boolean
    suspend fun lockAccount(email: String, userType: UserType, lockDurationMinutes: Long): Boolean
    suspend fun getAttemptCount(email: String, userType: UserType): Int
}

class LoginAttemptRepositoryImpl : LoginAttemptRepository {

    private val attemptPredicate = { email: String, userType: UserType ->
        (LoginAttemptTable.email eq email) and (LoginAttemptTable.userType eq userType.name)
    }

    override suspend fun getAttempt(email: String, userType: UserType): LoginAttemptDAO? = query {
        LoginAttemptDAO.find { attemptPredicate(email, userType) }.singleOrNull()
    }

    override suspend fun recordFailedAttempt(email: String, userType: UserType, ipAddress: String?): Int = query {
        val existing = LoginAttemptDAO.find { attemptPredicate(email, userType) }.singleOrNull()
        if (existing != null) {
            existing.attemptCount++
            existing.ipAddress = ipAddress
            existing.attemptCount
        } else {
            LoginAttemptDAO.new {
                this.email = email
                this.userType = userType.name
                this.ipAddress = ipAddress
                this.attemptCount = 1
            }
            1
        }
    }

    override suspend fun resetAttempts(email: String, userType: UserType): Boolean = query {
        LoginAttemptDAO.find { attemptPredicate(email, userType) }.singleOrNull()?.apply {
            attemptCount = 0
            lockedUntil = null
            ipAddress = null
        }
        true
    }

    override suspend fun lockAccount(email: String, userType: UserType, lockDurationMinutes: Long): Boolean = query {
        LoginAttemptDAO.find { attemptPredicate(email, userType) }.singleOrNull()?.apply {
            lockedUntil = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(lockDurationMinutes)
                .atZone(ZoneOffset.UTC).toInstant()
        } != null
    }

    override suspend fun getAttemptCount(email: String, userType: UserType): Int = query {
        LoginAttemptDAO.find { attemptPredicate(email, userType) }.singleOrNull()?.attemptCount ?: 0
    }
}
