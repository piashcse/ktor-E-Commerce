package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.database.entities.*
import com.piashcse.model.request.*
import com.piashcse.utils.common.AppClock
import com.piashcse.utils.extension.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import java.util.*

/** Registration/reset OTP attempt tracking and lockouts. */
class OtpAttemptService {
    fun registrationAttempt(
        userId: EntityID<String>,
        forUpdate: Boolean = false,
    ) = (
        if (forUpdate) {
            OtpAttemptDAO.find {
                (OtpAttemptTable.userId eq userId) and (OtpAttemptTable.purpose eq "GENERAL")
            }.forUpdate()
        } else {
            OtpAttemptDAO.find { (OtpAttemptTable.userId eq userId) and (OtpAttemptTable.purpose eq "GENERAL") }
        }
    ).singleOrNull()

    fun resetAttempt(
        userId: EntityID<String>,
        forUpdate: Boolean = false,
    ) = (
        if (forUpdate) {
            OtpAttemptDAO.find { (OtpAttemptTable.userId eq userId) and (OtpAttemptTable.purpose eq "RESET") }.forUpdate()
        } else {
            OtpAttemptDAO.find { (OtpAttemptTable.userId eq userId) and (OtpAttemptTable.purpose eq "RESET") }
        }
    ).singleOrNull()

    fun recordLockedOtpFailure(
        user: UserDAO,
        attempt: OtpAttemptDAO?,
    ) {
        val record =
            attempt ?: OtpAttemptDAO.new {
                this.userId = user.id
                this.purpose = "GENERAL"
                this.attemptCount = 0
            }
        record.attemptCount = record.attemptCount + 1
        if (record.attemptCount >= AppConstants.Authentication.MAX_OTP_ATTEMPTS) {
            record.lockedUntil = AppClock.now().plusSeconds(AppConstants.Authentication.OTP_LOCKOUT_MINUTES * 60)
            user.otpCode = null
            user.otpExpiry = null
        }
    }

    suspend fun invalidateOtp(userId: String) =
        query {
            val userEntity = UserDAO.findById(userId) ?: return@query
            userEntity.otpCode = null
            userEntity.otpExpiry = null
        }

    // ── OTP attempt tracking (persistent) ─────────────────────────────────

    suspend fun getOtpAttempt(userId: String): Int =
        query {
            registrationAttempt(userId.entityID(UserTable))?.attemptCount ?: 0
        }

    suspend fun isOtpLocked(userId: String): Boolean =
        query {
            registrationAttempt(userId.entityID(UserTable))?.isLocked == true
        }

    suspend fun recordFailedOtpAttempt(userId: String): Int =
        query {
            val existing = registrationAttempt(userId.entityID(UserTable), forUpdate = true)
            if (existing != null) {
                existing.attemptCount++
                existing.attemptCount
            } else {
                OtpAttemptDAO.new {
                    this.userId = userId.entityID(UserTable)
                    this.purpose = "GENERAL"
                    this.attemptCount = 1
                }
                1
            }
        }

    suspend fun resetOtpAttempts(userId: String) {
        query {
            // Locked read + locked-aware reset: never delete a row carrying a live lock,
            // otherwise a concurrent lockOtpAttempts would be silently no-op'd.
            val existing = registrationAttempt(userId.entityID(UserTable), forUpdate = true)
            if (existing?.isLocked == true) return@query
            existing?.let {
                it.attemptCount = 0
                it.lockedUntil = null
            }
        }
    }

    suspend fun lockOtpAttempts(userId: String) {
        query {
            val id = userId.entityID(UserTable)
            val lockUntil = AppClock.now().plusSeconds(AppConstants.Authentication.OTP_LOCKOUT_MINUTES * 60)
            val existing = registrationAttempt(id, forUpdate = true)
            if (existing != null) {
                existing.lockedUntil = lockUntil
            } else {
                // Create the locked row so a concurrent reset cannot no-op this lock.
                OtpAttemptDAO.new {
                    this.userId = id
                    this.purpose = "GENERAL"
                    this.attemptCount = AppConstants.Authentication.MAX_OTP_ATTEMPTS
                    this.lockedUntil = lockUntil
                }
            }
        }
    }
}
