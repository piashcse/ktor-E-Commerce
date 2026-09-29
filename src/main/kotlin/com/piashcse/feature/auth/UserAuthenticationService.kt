package com.piashcse.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.database.entities.LoginResponse
import com.piashcse.event.EventBus
import com.piashcse.event.SendEmailEvent
import com.piashcse.event.UserRegisteredEvent
import com.piashcse.mapper.toUserResponse
import com.piashcse.model.request.ForgotPasswordRequest
import com.piashcse.model.request.LoginRequest
import com.piashcse.model.request.RegisterRequest
import com.piashcse.model.response.RegistrationResult
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.InvalidCredentialsException
import com.piashcse.utils.validator.ValidationException

class UserAuthenticationService(private val authRepo: AuthRepository) {
    suspend fun register(registerRequest: RegisterRequest): RegistrationResult {
        val userType =
            UserType.fromString(registerRequest.userType)
                ?: throw ValidationException(Message.Validation.INVALID_USER_TYPE)
        if (userType == UserType.ADMIN || userType == UserType.SUPER_ADMIN) {
            throw ValidationException(Message.Auth.REGISTRATION_ROLE_FORBIDDEN)
        }
        val result = authRepo.register(registerRequest)
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

    private suspend fun sendRegistrationOtp(
        userId: String,
        email: String,
    ) {
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
        val userTypeEnum =
            UserType.fromString(loginRequest.userType)
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
            EventBus.publishAdminAction(
                Triple(user.id.value, user.email, user.userType.name),
                "LOGIN_FAILED",
                "USER",
                user.id.value,
                "Failed login attempt",
            )
            if (attemptCount >= AppConstants.Authentication.MAX_LOGIN_ATTEMPTS) {
                authRepo.lockAccount(loginRequest.email, userTypeEnum, AppConstants.Authentication.ACCOUNT_LOCKOUT_MINUTES)
                throw ValidationException(Message.Auth.accountLocked(AppConstants.Authentication.ACCOUNT_LOCKOUT_MINUTES))
            }
            throw InvalidCredentialsException(remainingAttempts = AppConstants.Authentication.MAX_LOGIN_ATTEMPTS - attemptCount)
        }

        if (!user.isActive) throw ValidationException(Message.Auth.ACCOUNT_DEACTIVATED)
        if (!user.isVerified) throw ValidationException(Message.Auth.ACCOUNT_NOT_VERIFIED)

        authRepo.resetLoginAttempts(loginRequest.email, userTypeEnum)
        val tokenPair = authRepo.generateTokenPair(user.id.value, user.email, user.userType.name)
        authRepo.storeRefreshToken(user.id.value, tokenPair.refreshToken)
        return LoginResponse(user.toUserResponse(), tokenPair.accessToken, tokenPair.refreshToken, tokenPair.expiresIn)
    }

    suspend fun otpVerification(
        userId: String,
        otp: String,
    ): Boolean =
        // Single repository transaction (check + count + lock under row-level locks):
        // splitting these into separate isLocked / count / verify / lock calls reopens
        // the OTP TOCTOU race between concurrent verifiers.
        authRepo.verifyOtp(userId, otp)

    suspend fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest) {
        // Anti-enumeration: unknown email/role returns success without sending email.
        // Caller (route) always responds OTP_SENT, so attacker cannot oracle accounts.
        val user =
            runCatching {
                authRepo.findResetUserByEmail(forgotPasswordRequest.email, forgotPasswordRequest.userType)
            }.getOrNull() ?: return
        val otp = authRepo.forgotPassword(forgotPasswordRequest)
        EventBus.publish(
            SendEmailEvent(
                to = user.email,
                subject = AppConstants.SmtpServer.RESET_SUBJECT,
                body = "Your password reset code is: $otp",
            ),
        )
    }
}
