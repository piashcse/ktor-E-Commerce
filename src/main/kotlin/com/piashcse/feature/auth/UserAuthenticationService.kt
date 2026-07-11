package com.piashcse.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.database.entities.LoginResponse
import com.piashcse.mapper.toUserResponse
import com.piashcse.model.request.ForgotPasswordRequest
import com.piashcse.model.request.LoginRequest
import com.piashcse.model.request.RegisterRequest
import com.piashcse.model.response.RegistrationResult
import com.piashcse.utils.email.EmailSender
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.InvalidCredentialsException
import com.piashcse.utils.validator.ValidationException

class UserAuthenticationService(private val authRepo: AuthRepository) {
    companion object {
        private const val MAX_LOGIN_ATTEMPTS = 5
        private const val ACCOUNT_LOCKOUT_MINUTES = 30L
        private const val MAX_OTP_ATTEMPTS = 5
        private const val OTP_LOCKOUT_MINUTES = 30L
    }

    suspend fun register(registerRequest: RegisterRequest): RegistrationResult {
        if (UserType.fromString(registerRequest.userType) == null)
            throw ValidationException(Message.Validation.INVALID_USER_TYPE)
        val result = authRepo.register(registerRequest)
        val email = when (result) {
            is RegistrationResult.Created -> result.email
            is RegistrationResult.OtpResent -> registerRequest.email
        }
        EmailSender.sendOtp(email, "", "Account Verification")
        return result
    }

    suspend fun login(loginRequest: LoginRequest): LoginResponse {
        val userTypeEnum = UserType.fromString(loginRequest.userType)
            ?: throw ValidationException(Message.Validation.INVALID_USER_TYPE)

        authRepo.getLoginAttempt(loginRequest.email, userTypeEnum)?.let {
            if (it.isLocked) throw ValidationException(Message.Auth.accountLocked(ACCOUNT_LOCKOUT_MINUTES))
        }

        val user = authRepo.findUserByEmailAndType(loginRequest.email, userTypeEnum) ?: run {
            authRepo.recordFailedAttempt(loginRequest.email, userTypeEnum, null)
            loginRequest.email.throwNotFound("User")
        }

        if (!BCrypt.verifyer().verify(loginRequest.password.toCharArray(), user.password).verified) {
            val attemptCount = authRepo.recordFailedAttempt(loginRequest.email, userTypeEnum, null)
            if (attemptCount >= MAX_LOGIN_ATTEMPTS) {
                authRepo.lockAccount(loginRequest.email, userTypeEnum, ACCOUNT_LOCKOUT_MINUTES)
                throw ValidationException(Message.Auth.accountLocked(ACCOUNT_LOCKOUT_MINUTES))
            }
            throw InvalidCredentialsException(remainingAttempts = MAX_LOGIN_ATTEMPTS - attemptCount)
        }

        if (!user.isActive) throw ValidationException(Message.Auth.ACCOUNT_DEACTIVATED)
        if (!user.isVerified) throw ValidationException(Message.Auth.ACCOUNT_NOT_VERIFIED)

        authRepo.resetLoginAttempts(loginRequest.email, userTypeEnum)
        val tokenPair = authRepo.generateTokenPair(user.id.value, user.email, user.userType.name)
        authRepo.storeRefreshToken(user.id.value, tokenPair.refreshToken)
        return LoginResponse(user.toUserResponse(), tokenPair.accessToken, tokenPair.refreshToken, tokenPair.expiresIn)
    }

    suspend fun otpVerification(userId: String, otp: String): Boolean {
        val currentAttempts = authRepo.getOtpAttempt(userId)
        if (currentAttempts >= MAX_OTP_ATTEMPTS) {
            authRepo.invalidateOtp(userId)
            throw ValidationException(Message.Auth.OTP_INVALID)
        }
        val isValid = authRepo.verifyOtp(userId, otp)
        if (isValid) {
            authRepo.resetOtpAttempts(userId)
        } else {
            val newCount = authRepo.recordFailedOtpAttempt(userId)
            if (newCount >= MAX_OTP_ATTEMPTS) {
                authRepo.lockOtpAttempts(userId)
                authRepo.invalidateOtp(userId)
            }
        }
        return isValid
    }

    suspend fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest) {
        val user = authRepo.findResetUserByEmail(forgotPasswordRequest.email, forgotPasswordRequest.userType)
        authRepo.forgotPassword(forgotPasswordRequest)
        EmailSender.sendOtp(user.email, "", "Password Reset")
    }
}
