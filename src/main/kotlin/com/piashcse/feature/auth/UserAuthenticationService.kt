package com.piashcse.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.event.EventBus
import com.piashcse.event.SendEmailEvent
import com.piashcse.event.UserRegisteredEvent
import com.piashcse.mapper.toUserResponse
import com.piashcse.model.request.ChangePassword
import com.piashcse.model.request.ForgotPasswordRequest
import com.piashcse.model.request.LoginRequest
import com.piashcse.model.request.LogoutRequest
import com.piashcse.model.request.RefreshTokenRequest
import com.piashcse.model.request.RegisterRequest
import com.piashcse.model.request.ResetRequest
import com.piashcse.model.request.TokenPair
import com.piashcse.model.response.LoginResponse
import com.piashcse.model.response.RegistrationResult
import com.piashcse.model.response.ResetResult
import com.piashcse.service.CacheService
import com.piashcse.utils.common.constantTimeEquals
import com.piashcse.utils.common.generateOTP
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.InvalidCredentialsException
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import java.time.LocalDateTime

/**
 * Application service for authentication and account management.
 *
 * Owns all business logic for the auth feature: role validation, password
 * hashing/verification, login & OTP lockout policy, token rotation, account
 * management authorization, event publishing, and transaction boundaries.
 * The repository only persists rows and projects DTOs.
 */
class UserAuthenticationService(private val authRepo: AuthRepository) {

    suspend fun register(registerRequest: RegisterRequest): RegistrationResult {
        val userType = UserType.fromString(registerRequest.userType)
            ?: throw ValidationException(Message.Validation.INVALID_USER_TYPE)
        if (userType == UserType.ADMIN || userType == UserType.SUPER_ADMIN) {
            throw ValidationException(Message.Auth.REGISTRATION_ROLE_FORBIDDEN)
        }

        val result = suspendRetryQuery {
            val existing = authRepo.findUserByEmailAndType(registerRequest.email, userType)
            val otp = generateOTP()
            val otpExpiry = LocalDateTime.now().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES)

            if (existing != null) {
                if (existing.isVerified) throw ValidationException(Message.Auth.USER_EXISTS)
                if (authRepo.getRegistrationOtpExpiry(existing.id)?.isAfter(LocalDateTime.now()) == true) {
                    throw ValidationException(Message.Auth.OTP_ALREADY_SENT)
                }
                authRepo.resendRegistrationOtp(existing.id, otp, otpExpiry)
                RegistrationResult.OtpResent(existing.id, existing.email, Message.Auth.OTP_SENT)
            } else {
                val passwordHash = BCrypt.withDefaults().hashToString(AppConstants.BCRYPT_COST, registerRequest.password.toCharArray())
                val id = authRepo.createUserWithProfile(registerRequest.email, passwordHash, userType, otp, otpExpiry)
                RegistrationResult.Created(id, registerRequest.email, Message.Auth.OTP_SENT)
            }
        }

        when (result) {
            is RegistrationResult.Created -> {
                EventBus.publish(
                    UserRegisteredEvent(
                        userId = result.id,
                        email = result.email,
                        userType = registerRequest.userType,
                    ),
                )
                sendRegistrationOtp(result.id, result.email)
            }
            is RegistrationResult.OtpResent -> sendRegistrationOtp(result.id, result.email)
        }
        return result
    }

    private suspend fun sendRegistrationOtp(userId: String, email: String) {
        val otp = authRepo.getRegistrationOtp(userId)
        EventBus.publish(
            SendEmailEvent(
                to = email,
                subject = AppConstants.SmtpServer.OTP_SUBJECT,
                body = "Your verification code is: ${otp.orEmpty()}",
            ),
        )
    }

    suspend fun login(
        loginRequest: LoginRequest,
        ipAddress: String? = null,
    ): LoginResponse {
        val userTypeEnum = UserType.fromString(loginRequest.userType)
            ?: throw ValidationException(Message.Validation.INVALID_USER_TYPE)

        authRepo.getLoginAttempt(loginRequest.email, userTypeEnum)?.let {
            if (it.isLocked) throw ValidationException(Message.Auth.accountLocked(AppConstants.Authentication.ACCOUNT_LOCKOUT_MINUTES))
            if (it.attemptCount >= AppConstants.Authentication.MAX_LOGIN_ATTEMPTS) {
                authRepo.resetLoginAttempts(loginRequest.email, userTypeEnum)
            }
        }

        val user = authRepo.findUserByEmailAndType(loginRequest.email, userTypeEnum)
        if (user == null) {
            val attemptCount = authRepo.recordFailedAttempt(loginRequest.email, userTypeEnum, ipAddress)
            if (attemptCount >= AppConstants.Authentication.MAX_LOGIN_ATTEMPTS) {
                authRepo.lockAccount(loginRequest.email, userTypeEnum, AppConstants.Authentication.ACCOUNT_LOCKOUT_MINUTES)
                throw ValidationException(Message.Auth.accountLocked(AppConstants.Authentication.ACCOUNT_LOCKOUT_MINUTES))
            }
            throw InvalidCredentialsException(remainingAttempts = AppConstants.Authentication.MAX_LOGIN_ATTEMPTS - attemptCount)
        }

        if (!BCrypt.verifyer().verify(loginRequest.password.toCharArray(), user.password).verified) {
            val attemptCount = authRepo.recordFailedAttempt(loginRequest.email, userTypeEnum, ipAddress)
            if (attemptCount >= AppConstants.Authentication.MAX_LOGIN_ATTEMPTS) {
                authRepo.lockAccount(loginRequest.email, userTypeEnum, AppConstants.Authentication.ACCOUNT_LOCKOUT_MINUTES)
                throw ValidationException(Message.Auth.accountLocked(AppConstants.Authentication.ACCOUNT_LOCKOUT_MINUTES))
            }
            throw InvalidCredentialsException(remainingAttempts = AppConstants.Authentication.MAX_LOGIN_ATTEMPTS - attemptCount)
        }

        if (!user.isActive) throw ValidationException(Message.Auth.ACCOUNT_DEACTIVATED)
        if (!user.isVerified) throw ValidationException(Message.Auth.ACCOUNT_NOT_VERIFIED)

        authRepo.resetLoginAttempts(loginRequest.email, userTypeEnum)
        val tokenPair = authRepo.generateTokenPair(user.id, user.email, user.userType.name)
        authRepo.storeRefreshToken(user.id, tokenPair.refreshToken)
        return LoginResponse(user.toUserResponse(), tokenPair.accessToken, tokenPair.refreshToken, tokenPair.expiresIn)
    }

    suspend fun otpVerification(userId: String, otp: String): Boolean = suspendRetryQuery {
        if (authRepo.findUserById(userId) == null) userId.throwNotFound("User")
        if (authRepo.isOtpLocked(userId)) {
            throw ValidationException(Message.Auth.accountLocked(AppConstants.Authentication.OTP_LOCKOUT_MINUTES))
        }
        if (authRepo.getOtpAttempt(userId) >= AppConstants.Authentication.MAX_OTP_ATTEMPTS) {
            authRepo.resetOtpAttempts(userId)
        }

        val expiry = authRepo.getRegistrationOtpExpiry(userId)
        val code = authRepo.getRegistrationOtp(userId)
        val isValid = expiry?.isAfter(LocalDateTime.now()) == true && constantTimeEquals(code.orEmpty(), otp)

        if (isValid) {
            authRepo.markUserVerified(userId)
            authRepo.resetOtpAttempts(userId)
        } else {
            val newCount = authRepo.recordFailedOtpAttempt(userId)
            if (newCount >= AppConstants.Authentication.MAX_OTP_ATTEMPTS) {
                authRepo.lockOtpAttempts(userId)
                authRepo.invalidateOtp(userId)
            }
        }
        isValid
    }

    suspend fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest) = suspendRetryQuery {
        val user = authRepo.findResetUserByEmail(forgotPasswordRequest.email, forgotPasswordRequest.userType)
        val otp = generateOTP()
        authRepo.setResetOtp(user.id, otp, LocalDateTime.now().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES))
        user.email to otp
    }.let { (email, otp) ->
        EventBus.publish(
            SendEmailEvent(
                to = email,
                subject = AppConstants.SmtpServer.RESET_SUBJECT,
                body = "Your password reset code is: $otp",
            ),
        )
    }

    suspend fun resetPassword(resetPasswordRequest: ResetRequest): ResetResult = suspendRetryQuery {
        val user = authRepo.findResetUserByEmail(resetPasswordRequest.email, resetPasswordRequest.userType)

        if (authRepo.isOtpLocked(user.id)) return@suspendRetryQuery ResetResult.Locked

        val resetOtp = authRepo.getResetOtp(user.id)
        if (resetOtp?.expiry?.isBefore(LocalDateTime.now()) != false) {
            return@suspendRetryQuery ResetResult.InvalidOrExpiredOtp
        }

        if (!constantTimeEquals(resetOtp.code.orEmpty(), resetPasswordRequest.verificationCode)) {
            val attempt = authRepo.recordFailedOtpAttempt(user.id)
            if (attempt >= AppConstants.Authentication.MAX_OTP_ATTEMPTS) {
                authRepo.lockOtpAttempts(user.id)
                authRepo.clearResetOtp(user.id)
            }
            return@suspendRetryQuery ResetResult.InvalidOrExpiredOtp
        }

        if (BCrypt.verifyer().verify(resetPasswordRequest.newPassword.toCharArray(), user.password).verified) {
            throw ValidationException(Message.Auth.PASSWORD_SAME)
        }

        authRepo.updatePasswordHash(
            user.id,
            BCrypt.withDefaults().hashToString(AppConstants.BCRYPT_COST, resetPasswordRequest.newPassword.toCharArray()),
        )
        authRepo.clearResetOtp(user.id)
        authRepo.resetOtpAttempts(user.id)
        ResetResult.Success
    }

    suspend fun refreshAccessToken(request: RefreshTokenRequest): TokenPair = suspendRetryQuery {
        val tokenHash = authRepo.hashRefreshToken(request.refreshToken)
        val storedToken = authRepo.getRefreshTokenByHash(tokenHash)
            ?: throw NotFoundException(Message.Auth.INVALID_REFRESH_TOKEN)

        if (!storedToken.isValid) {
            authRepo.revokeRefreshToken(tokenHash)
            throw NotFoundException(Message.Auth.TOKEN_EXPIRED)
        }

        val user = authRepo.findUserById(storedToken.userId) ?: storedToken.userId.throwNotFound("User")
        if (!user.isActive) throw ValidationException(Message.Auth.ACCOUNT_DEACTIVATED)
        if (!user.isVerified) throw ValidationException(Message.Auth.ACCOUNT_NOT_VERIFIED)

        authRepo.revokeRefreshToken(tokenHash)
        val newTokenPair = authRepo.generateTokenPair(user.id, user.email, user.userType.name)
        authRepo.storeRefreshToken(user.id, newTokenPair.refreshToken)
        newTokenPair
    }

    suspend fun logout(
        userId: String,
        refreshToken: String?,
    ): Boolean = suspendRetryQuery {
        if (!refreshToken.isNullOrBlank()) {
            authRepo.revokeRefreshToken(authRepo.hashRefreshToken(refreshToken))
        } else {
            authRepo.revokeAllUserTokens(userId)
        }
        true
    }

    suspend fun blacklistToken(token: String): Boolean {
        suspendRetryQuery { authRepo.insertBlacklistedToken(token) }
        CacheService.cache.set("blacklisted_token:$token", true, AppConstants.Authentication.JWT_EXPIRY_SECONDS)
        return true
    }

    suspend fun changePassword(
        userId: String,
        changePassword: ChangePassword,
    ): Boolean = suspendRetryQuery {
        val user = authRepo.findUserById(userId) ?: userId.throwNotFound("User")
        if (!BCrypt.verifyer().verify(changePassword.oldPassword.toCharArray(), user.password).verified) return@suspendRetryQuery false
        if (changePassword.oldPassword == changePassword.newPassword) throw ValidationException(Message.Auth.PASSWORD_SAME)

        authRepo.updatePasswordHash(
            userId,
            BCrypt.withDefaults().hashToString(AppConstants.BCRYPT_COST, changePassword.newPassword.toCharArray()),
        )
        true
    }

    suspend fun changeUserType(
        currentUserId: String,
        targetUserId: String,
        newUserType: UserType,
    ): Boolean = suspendRetryQuery {
        currentUserId.requireNotBlank("Current User ID")
        targetUserId.requireNotBlank("Target User ID")

        val currentUser = authRepo.findUserById(currentUserId) ?: currentUserId.throwNotFound("User")
        val targetUser = authRepo.findUserById(targetUserId) ?: targetUserId.throwNotFound("User")

        if (!currentUser.userType.canManage(targetUser.userType)) {
            throw ValidationException(Message.Auth.insufficientPermissions("change user type to $newUserType"))
        }
        if (currentUser.id == targetUser.id) {
            throw ValidationException(Message.Auth.insufficientPermissions("change your own user type"))
        }
        if (!currentUser.userType.canManage(newUserType)) {
            throw ValidationException(Message.Auth.insufficientPermissions("change user type to $newUserType"))
        }

        authRepo.updateUserType(targetUserId, newUserType)
        if (newUserType == UserType.SELLER) authRepo.createSellerProfileIfMissing(targetUserId)
        true
    }

    suspend fun deactivateUser(
        currentUserId: String,
        targetUserId: String,
    ): Boolean = suspendRetryQuery {
        currentUserId.requireNotBlank("Current User ID")
        targetUserId.requireNotBlank("Target User ID")

        val currentUser = authRepo.findUserById(currentUserId) ?: currentUserId.throwNotFound("User")
        val targetUser = authRepo.findUserById(targetUserId) ?: targetUserId.throwNotFound("User")

        if (!currentUser.userType.canManage(targetUser.userType)) {
            throw ValidationException(Message.Auth.insufficientPermissions("deactivate user"))
        }

        authRepo.setUserActive(targetUserId, false)
        authRepo.revokeAllUserTokens(targetUserId)
        true
    }

    suspend fun activateUser(
        currentUserId: String,
        targetUserId: String,
    ): Boolean = suspendRetryQuery {
        currentUserId.requireNotBlank("Current User ID")
        targetUserId.requireNotBlank("Target User ID")

        val currentUser = authRepo.findUserById(currentUserId) ?: currentUserId.throwNotFound("User")
        val targetUser = authRepo.findUserById(targetUserId) ?: targetUserId.throwNotFound("User")

        if (!currentUser.userType.canManage(targetUser.userType)) {
            throw ValidationException(Message.Auth.insufficientPermissions("activate user"))
        }

        authRepo.setUserActive(targetUserId, true)
        true
    }
}
