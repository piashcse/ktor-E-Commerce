package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.model.request.*
import com.piashcse.utils.common.constantTimeEquals
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq
import java.util.*

/** OTP verification and account-claim transition. */
class OtpVerificationService {
    private val tokens = AuthTokenService()
    private val otpAttempts = OtpAttemptService()

    suspend fun verifyOtp(
        userId: String,
        otp: String,
    ): Boolean =
        query {
            // Single-transaction check + count + lock under row-level locks. The attempt row
            // and the user row are both locked here, closing the TOCTOU window that separate
            // isLocked / count / verify / lock transactions left open to concurrent verifiers.
            val userEntity =
                UserDAO.find { UserTable.id eq userId.entityID(UserTable) }.forUpdate().singleOrNull()
                    ?: throw NotFoundException(Message.Errors.NOT_FOUND)
            val attempt = otpAttempts.registrationAttempt(userEntity.id, forUpdate = true)
            if (attempt?.isLocked == true) {
                throw ValidationException(Message.Auth.accountLocked(AppConstants.Authentication.OTP_LOCKOUT_MINUTES))
            }
            if (attempt != null && (attempt.isLockExpired || attempt.attemptCount >= AppConstants.Authentication.MAX_OTP_ATTEMPTS)) {
                // Time-based lockout: an expired (or stale saturated) counter decays instead of locking forever.
                attempt.attemptCount = 0
                attempt.lockedUntil = null
            }
            if (userEntity.otpExpiry?.isBefore(currentUtc()) != false ||
                !constantTimeEquals(userEntity.otpCode.orEmpty(), tokens.hashOtp(otp))
            ) {
                otpAttempts.recordLockedOtpFailure(userEntity, attempt)
                return@query false
            }
            userEntity.isVerified = true
            // A guest checkout account becomes a normal account after OTP verification.
            userEntity.isGuest = false
            userEntity.otpCode = null
            userEntity.otpExpiry = null
            attempt?.let {
                it.attemptCount = 0
                it.lockedUntil = null
            }
            true
        }
}
