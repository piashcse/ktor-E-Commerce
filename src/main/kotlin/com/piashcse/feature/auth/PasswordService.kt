package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.database.entities.ChangePassword
import com.piashcse.database.entities.OtpAttemptDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.model.request.ForgotPasswordRequest
import com.piashcse.model.request.ResetRequest
import com.piashcse.model.response.ResetResult
import com.piashcse.utils.common.AppClock
import com.piashcse.utils.common.constantTimeEquals
import com.piashcse.utils.common.generateOTP
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq

/** Password change, reset-OTP issuance and reset completion. */
class PasswordService {
    private val tokens = AuthTokenService()
    private val otpAttempts = OtpAttemptService()

    private fun normalizeEmail(value: String): String = value.trim().lowercase()

    private fun userTypeOrNull(raw: String): UserType? = runCatching { UserType.valueOf(raw.uppercase()) }.getOrNull()

    suspend fun changePassword(
        userId: String,
        changePassword: ChangePassword,
    ): Boolean =
        query {
            val userEntity = UserDAO.findById(userId) ?: throw NotFoundException(Message.Errors.NOT_FOUND)
            if (!PasswordHasher.verify(changePassword.oldPassword, userEntity.password)) return@query false
            if (changePassword.oldPassword == changePassword.newPassword) throw ValidationException(Message.Auth.PASSWORD_SAME)
            userEntity.password = PasswordHasher.hash(changePassword.newPassword)
            userEntity.tokenVersion += 1
            // A password change must invalidate every existing session.
            tokens.revokeAllUserTokensTx(userId)
            true
        }

    suspend fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest): String =
        query {
            val entities = UserDAO.find { UserTable.email eq normalizeEmail(forgotPasswordRequest.email) }.toList()
            if (entities.isEmpty()) forgotPasswordRequest.email.throwNotFound("User")
            val type =
                userTypeOrNull(forgotPasswordRequest.userType)
                    ?: throw NotFoundException(Message.Auth.userNotFoundForRole(forgotPasswordRequest.userType))
            val user =
                entities.find { it.userType == type }
                    ?: throw NotFoundException(Message.Auth.userNotFoundForRole(forgotPasswordRequest.userType))
            val otp = generateOTP()
            user.resetOtpCode = tokens.hashOtp(otp)
            user.resetOtpExpiry = currentUtc().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES)
            otp
        }

    suspend fun resetPassword(resetPasswordRequest: ResetRequest): ResetResult =
        query {
            // Anti-enumeration: unknown email/role behaves exactly like a bad OTP.
            val entities = UserDAO.find { UserTable.email eq normalizeEmail(resetPasswordRequest.email) }.toList()
            if (entities.isEmpty()) return@query ResetResult.InvalidOrExpiredOtp
            val type = userTypeOrNull(resetPasswordRequest.userType) ?: return@query ResetResult.InvalidOrExpiredOtp
            val user =
                entities.find { it.userType == type } ?: return@query ResetResult.InvalidOrExpiredOtp

            val otpAttempt = otpAttempts.resetAttempt(user.id, forUpdate = true)
            if (otpAttempt?.isLocked == true) return@query ResetResult.Locked

            if (user.resetOtpExpiry?.isBefore(currentUtc()) != false) {
                return@query ResetResult.InvalidOrExpiredOtp
            }

            if (!constantTimeEquals(user.resetOtpCode.orEmpty(), tokens.hashOtp(resetPasswordRequest.verificationCode))) {
                val attemptRecord =
                    if (otpAttempt != null) {
                        otpAttempt.attemptCount = otpAttempt.attemptCount + 1
                        otpAttempt
                    } else {
                        OtpAttemptDAO.new {
                            this.userId = user.id
                            this.purpose = "RESET"
                            this.attemptCount = 1
                        }
                    }
                if (attemptRecord.attemptCount >= AppConstants.Authentication.MAX_OTP_ATTEMPTS) {
                    attemptRecord.lockedUntil = AppClock.now().plusSeconds(AppConstants.Authentication.OTP_LOCKOUT_MINUTES * 60)
                    user.resetOtpCode = null
                    user.resetOtpExpiry = null
                }
                return@query ResetResult.InvalidOrExpiredOtp
            }

            if (PasswordHasher.verify(resetPasswordRequest.newPassword, user.password)) {
                throw ValidationException(Message.Auth.PASSWORD_SAME)
            }

            // Update the attempt row in place: never delete a row that may carry a live lock,
            // otherwise a concurrent locker would be silently no-op'd.
            otpAttempt?.let {
                it.attemptCount = 0
                it.lockedUntil = null
            }
            user.resetOtpCode = null
            user.resetOtpExpiry = null
            user.password = PasswordHasher.hash(resetPasswordRequest.newPassword)
            user.tokenVersion += 1
            // Password reset must invalidate every existing session.
            tokens.revokeAllUserTokensTx(user.id.value)
            ResetResult.Success
        }

    // ── OTP ───────────────────────────────────────────────────────────────
}
