package com.piashcse.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.model.request.*
import com.piashcse.model.response.RegistrationResult
import com.piashcse.model.response.ResetResult
import com.piashcse.utils.common.generateOTP
import com.piashcse.utils.email.EmailSender
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.InvalidCredentialsException
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import com.piashcse.utils.validator.requireValidEmail
import com.piashcse.utils.validator.requireValidPassword
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class AuthService : AuthRepository {
    companion object {
        private const val REFRESH_TOKEN_EXPIRY_SECONDS = 7L * 24 * 60 * 60
        private const val MAX_LOGIN_ATTEMPTS = 5
        private const val ACCOUNT_LOCKOUT_MINUTES = 30L
    }

    // ── Token helpers ─────────────────────────────────────────────────────

    private fun hashRefreshToken(token: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun generateTokenPair(userId: String, email: String, userType: String): TokenPair {
        val accessToken = JwtConfig.tokenProvider(JwtTokenRequest(userId, email, userType))
        return TokenPair(accessToken = accessToken, refreshToken = UUID.randomUUID().toString(), expiresIn = 900)
    }

    private suspend fun storeRefreshToken(userId: String, refreshToken: String) {
        val tokenHash = hashRefreshToken(refreshToken)
        val expiresAt = Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRY_SECONDS)
        query {
            RefreshTokenDAO.new {
                this.userId = EntityID(userId, UserTable)
                this.tokenHash = tokenHash
                this.expiresAt = expiresAt
            }
        }
    }

    // ── Data helpers ──────────────────────────────────────────────────────

    private fun loginAttemptPredicate(email: String, userType: UserType) =
        (LoginAttemptTable.email eq email) and (LoginAttemptTable.userType eq userType.name)

    private suspend fun getRefreshTokenByHash(tokenHash: String): RefreshTokenDAO? = query {
        RefreshTokenDAO.find { RefreshTokenTable.tokenHash eq tokenHash }.singleOrNull()
    }

    private suspend fun revokeRefreshToken(tokenHash: String): Boolean = query {
        RefreshTokenDAO.find { RefreshTokenTable.tokenHash eq tokenHash }.singleOrNull()
            ?.let { it.revokedAt = Instant.now(); true } ?: false
    }

    private suspend fun revokeAllUserTokens(userId: String): Boolean = query {
        RefreshTokenDAO.find { RefreshTokenTable.userId eq EntityID(userId, UserTable) }
            .forEach { it.revokedAt = Instant.now() }
        true
    }

    private suspend fun recordFailedAttempt(email: String, userType: UserType, ipAddress: String?): Int = query {
        val existing = LoginAttemptDAO.find { loginAttemptPredicate(email, userType) }.singleOrNull()
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

    private suspend fun resetLoginAttempts(email: String, userType: UserType) = query {
        LoginAttemptDAO.find { loginAttemptPredicate(email, userType) }.singleOrNull()?.apply {
            attemptCount = 0; lockedUntil = null; ipAddress = null
        }
    }

    private suspend fun getLoginAttempt(email: String, userType: UserType): LoginAttemptDAO? = query {
        LoginAttemptDAO.find { loginAttemptPredicate(email, userType) }.singleOrNull()
    }

    private suspend fun lockAccount(email: String, userType: UserType, lockDurationMinutes: Long): Boolean = query {
        LoginAttemptDAO.find { loginAttemptPredicate(email, userType) }.singleOrNull()
            ?.apply { lockedUntil = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(lockDurationMinutes).atZone(ZoneOffset.UTC).toInstant() } != null
    }

    // ── Registration ──────────────────────────────────────────────────────

    override suspend fun register(registerRequest: RegisterRequest): RegistrationResult = query {
        validateRegisterRequest(registerRequest)

        val userTypeEnum = runCatching { UserType.valueOf(registerRequest.userType.uppercase()) }
            .getOrDefault(UserType.CUSTOMER)

        val existingUser =
            UserDAO.find { UserTable.email eq registerRequest.email and (UserTable.userType eq userTypeEnum) }
                .firstOrNull()

        val otp = generateOTP()
        val otpExpiryTime = LocalDateTime.now().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES)

        if (existingUser != null) {
            if (existingUser.isVerified) throw ValidationException(Message.Auth.USER_EXISTS)
            if (existingUser.otpExpiry != null && existingUser.otpExpiry!!.isAfter(LocalDateTime.now())) {
                throw ValidationException(Message.Auth.OTP_ALREADY_SENT)
            }
            existingUser.otpCode = otp
            existingUser.otpExpiry = otpExpiryTime
            EmailSender.sendOtp(existingUser.email, otp, "Account Verification")
            RegistrationResult.OtpResent(Message.Auth.OTP_SENT)
        } else {
            val inserted = UserDAO.new {
                email = registerRequest.email
                otpCode = otp
                otpExpiry = otpExpiryTime
                password = BCrypt.withDefaults().hashToString(AppConstants.BCRYPT_COST, registerRequest.password.toCharArray())
                userType = userTypeEnum
            }
            UserProfileDAO.new { userId = inserted.id }
            if (userTypeEnum == UserType.SELLER) {
                SellerDAO.new { userId = inserted.id; status = ShopStatus.PENDING }
            }
            EmailSender.sendOtp(inserted.email, otp, "Account Verification")
            RegistrationResult.Created(inserted.id.value, registerRequest.email, Message.Auth.OTP_SENT)
        }
    }

    private fun validateRegisterRequest(request: RegisterRequest) {
        requireValidEmail(request.email)
        requireValidPassword(request.password)
        if (UserType.fromString(request.userType) == null) throw ValidationException(Message.Validation.INVALID_USER_TYPE)
    }

    // ── Login ─────────────────────────────────────────────────────────────

    override suspend fun login(loginRequest: LoginRequest): LoginResponse {
        validateLoginRequest(loginRequest)
        val userTypeEnum = UserType.fromString(loginRequest.userType) ?: loginRequest.email.throwNotFound("User")
        checkAccountLockout(loginRequest.email, userTypeEnum)

        val user = findUserByEmailAndType(loginRequest.email, userTypeEnum) ?: run {
            recordFailedAttempt(loginRequest.email, userTypeEnum, null)
            loginRequest.email.throwNotFound("User")
        }

        if (!isPasswordValid(loginRequest.password, user.password)) handleFailedLogin(loginRequest.email, userTypeEnum)
        validateUserState(user)

        resetLoginAttempts(loginRequest.email, userTypeEnum)
        return issueTokensAndLogin(user)
    }

    private fun validateLoginRequest(request: LoginRequest) {
        requireValidEmail(request.email)
        if (UserType.fromString(request.userType) == null) throw ValidationException(Message.Validation.INVALID_USER_TYPE)
        if (request.password.isBlank()) throw ValidationException(Message.Validation.EMPTY_PASSWORD)
    }

    private suspend fun checkAccountLockout(email: String, userTypeEnum: UserType) {
        getLoginAttempt(email, userTypeEnum)?.let {
            if (it.isLocked) throw ValidationException(Message.Auth.accountLocked(ACCOUNT_LOCKOUT_MINUTES))
        }
    }

    private suspend fun findUserByEmailAndType(email: String, userTypeEnum: UserType): UserDAO? = query {
        UserDAO.find { UserTable.email eq email and (UserTable.userType eq userTypeEnum) }.firstOrNull()
    }

    private fun isPasswordValid(rawPassword: String, hashedPassword: String): Boolean =
        BCrypt.verifyer().verify(rawPassword.toCharArray(), hashedPassword).verified

    private suspend fun handleFailedLogin(email: String, userTypeEnum: UserType) {
        val attemptCount = recordFailedAttempt(email, userTypeEnum, null)
        if (attemptCount >= MAX_LOGIN_ATTEMPTS) {
            lockAccount(email, userTypeEnum, ACCOUNT_LOCKOUT_MINUTES)
            throw ValidationException(Message.Auth.accountLocked(ACCOUNT_LOCKOUT_MINUTES))
        }
        throw InvalidCredentialsException(remainingAttempts = MAX_LOGIN_ATTEMPTS - attemptCount)
    }

    private fun validateUserState(user: UserDAO) {
        when {
            !user.isActive -> throw ValidationException(Message.Auth.ACCOUNT_DEACTIVATED)
            !user.isVerified -> throw ValidationException(Message.Auth.ACCOUNT_NOT_VERIFIED)
        }
    }

    private suspend fun issueTokensAndLogin(user: UserDAO): LoginResponse {
        val tokenPair = generateTokenPair(user.id.value, user.email, user.userType.name)
        storeRefreshToken(user.id.value, tokenPair.refreshToken)
        return LoginResponse(user.response(), tokenPair.accessToken, tokenPair.refreshToken, tokenPair.expiresIn)
    }

    // ── OTP Verification ──────────────────────────────────────────────────

    override suspend fun otpVerification(userId: String, otp: String): Boolean = query {
        val userEntity = UserDAO.findById(userId) ?: throw NotFoundException(Message.Errors.NOT_FOUND)
        if (userEntity.otpCode == otp) { userEntity.isVerified = true; true } else false
    }

    // ── Change Password ───────────────────────────────────────────────────

    override suspend fun changePassword(userId: String, changePassword: ChangePassword): Boolean = query {
        val userEntity = UserDAO.findById(userId) ?: throw NotFoundException(Message.Errors.NOT_FOUND)
        if (!BCrypt.verifyer().verify(changePassword.oldPassword.toCharArray(), userEntity.password).verified) return@query false
        if (changePassword.oldPassword == changePassword.newPassword) throw ValidationException(Message.Auth.PASSWORD_SAME)
        userEntity.password = BCrypt.withDefaults().hashToString(AppConstants.BCRYPT_COST, changePassword.newPassword.toCharArray())
        true
    }

    // ── Forgot / Reset Password ──────────────────────────────────────────

    private fun findResetUserByEmail(email: String, userTypeStr: String): UserDAO {
        val entities = UserDAO.find { UserTable.email eq email }.toList()
        if (entities.isEmpty()) email.throwNotFound("User")
        val type = runCatching { UserType.valueOf(userTypeStr.uppercase()) }
            .getOrElse { throw NotFoundException(Message.Auth.userNotFoundForRole(email, userTypeStr)) }
        return entities.find { it.userType == type }
            ?: throw NotFoundException(Message.Auth.userNotFoundForRole(email, userTypeStr))
    }

    override suspend fun forgetPassword(forgetPasswordRequest: ForgetPasswordRequest): String = query {
        val user = findResetUserByEmail(forgetPasswordRequest.email, forgetPasswordRequest.userType)
        val otp = generateOTP()
        user.resetOtpCode = otp
        user.resetOtpExpiry = LocalDateTime.now().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES)
        EmailSender.sendOtp(user.email, otp, "Password Reset")
        otp
    }

    override suspend fun resetPassword(resetPasswordRequest: ResetRequest): ResetResult = query {
        val user = findResetUserByEmail(resetPasswordRequest.email, resetPasswordRequest.userType)

        if (user.resetOtpExpiry == null || user.resetOtpExpiry!!.isBefore(LocalDateTime.now()))
            return@query ResetResult.InvalidOrExpiredOtp

        if (user.resetOtpCode != resetPasswordRequest.verificationCode) return@query ResetResult.InvalidOrExpiredOtp

        requireValidPassword(resetPasswordRequest.newPassword)
        if (BCrypt.verifyer().verify(resetPasswordRequest.newPassword.toCharArray(), user.password).verified)
            throw ValidationException(Message.Auth.PASSWORD_SAME)

        user.password = BCrypt.withDefaults().hashToString(AppConstants.BCRYPT_COST, resetPasswordRequest.newPassword.toCharArray())
        ResetResult.Success
    }

    // ── Refresh Token ─────────────────────────────────────────────────────

    override suspend fun refreshAccessToken(request: RefreshTokenRequest): TokenPair {
        val tokenHash = hashRefreshToken(request.refreshToken)
        val storedToken = getRefreshTokenByHash(tokenHash)
            ?: throw NotFoundException(Message.Auth.INVALID_REFRESH_TOKEN)

        if (!storedToken.isValid) {
            revokeRefreshToken(tokenHash)
            throw NotFoundException(Message.Auth.TOKEN_EXPIRED)
        }

        val user = query { UserDAO.findById(storedToken.userId.value) ?: throw NotFoundException(Message.Errors.NOT_FOUND) }

        revokeRefreshToken(tokenHash)
        val newTokenPair = generateTokenPair(user.id.value, user.email, user.userType.name)
        storeRefreshToken(user.id.value, newTokenPair.refreshToken)
        return newTokenPair
    }

    // ── Logout / Blacklist ────────────────────────────────────────────────

    override suspend fun logout(userId: String, refreshToken: String?): Boolean {
        if (!refreshToken.isNullOrBlank()) {
            revokeRefreshToken(hashRefreshToken(refreshToken))
        } else {
            revokeAllUserTokens(userId)
        }
        return true
    }

    override suspend fun blacklistToken(token: String): Boolean = query {
        if (BlacklistedTokenDAO.find { BlacklistedTokenTable.token eq token }.firstOrNull() == null) {
            BlacklistedTokenDAO.new { this.token = token; this.blacklistedAt = Instant.now() }
        }
        true
    }

    // ── Admin: User Management ────────────────────────────────────────────

    private suspend fun <T> withUsers(
        currentUserId: String,
        targetUserId: String,
        action: String,
        block: (currentUser: UserDAO, targetUser: UserDAO) -> T,
    ): T = query {
        if (currentUserId.isBlank()) throw ValidationException("Current user ID cannot be blank")
        if (targetUserId.isBlank()) throw ValidationException("Target user ID cannot be blank")

        val currentUser = UserDAO.findById(currentUserId) ?: throw NotFoundException(Message.Errors.NOT_FOUND)
        val targetUser = UserDAO.findById(targetUserId) ?: throw NotFoundException(Message.Errors.NOT_FOUND)

        if (!currentUser.userType.canManage(targetUser.userType)) {
            throw ValidationException(Message.Auth.insufficientPermissions(action))
        }
        block(currentUser, targetUser)
    }

    override suspend fun changeUserType(currentUserId: String, targetUserId: String, newUserType: UserType): Boolean =
        withUsers(currentUserId, targetUserId, "change user type to $newUserType") { _, targetUser ->
            targetUser.userType = newUserType
            if (newUserType == UserType.SELLER && SellerDAO.find { SellerTable.userId eq targetUser.id }.firstOrNull() == null) {
                SellerDAO.new { userId = targetUser.id; status = ShopStatus.PENDING }
            }
            true
        }

    override suspend fun deactivateUser(currentUserId: String, targetUserId: String): Boolean =
        withUsers(currentUserId, targetUserId, "deactivate user") { _, targetUser ->
            targetUser.isActive = false
            true
        }

    override suspend fun activateUser(currentUserId: String, targetUserId: String): Boolean =
        withUsers(currentUserId, targetUserId, "activate user") { _, targetUser ->
            targetUser.isActive = true
            true
        }
}
