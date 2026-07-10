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
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class AuthRepositoryImpl : AuthRepository {
    companion object {
        private const val REFRESH_TOKEN_EXPIRY_SECONDS = 7L * 24 * 60 * 60
        private const val MAX_LOGIN_ATTEMPTS = 5
        private const val ACCOUNT_LOCKOUT_MINUTES = 30L
    }

    // ── Token helpers ─────────────────────────────────────────────────────

    override fun generateTokenPair(userId: String, email: String, userType: String): TokenPair {
        val accessToken = JwtConfig.tokenProvider(JwtTokenRequest(userId, email, userType))
        return TokenPair(accessToken = accessToken, refreshToken = UUID.randomUUID().toString(), expiresIn = 900)
    }

    private fun hashRefreshToken(token: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    override suspend fun storeRefreshToken(userId: String, refreshToken: String) {
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

    override suspend fun getRefreshTokenByHash(tokenHash: String): RefreshTokenDAO? = query {
        RefreshTokenDAO.find { RefreshTokenTable.tokenHash eq tokenHash }.singleOrNull()
    }

    override suspend fun revokeRefreshToken(tokenHash: String): Boolean = query {
        RefreshTokenDAO.find { RefreshTokenTable.tokenHash eq tokenHash }.singleOrNull()
            ?.let { it.revokedAt = Instant.now(); true } ?: false
    }

    override suspend fun revokeAllUserTokens(userId: String): Boolean = query {
        RefreshTokenDAO.find { RefreshTokenTable.userId eq EntityID(userId, UserTable) }
            .forEach { it.revokedAt = Instant.now() }
        true
    }

    // ── Login attempt helpers ────────────────────────────────────────────

    private fun loginAttemptPredicate(email: String, userType: UserType) =
        (LoginAttemptTable.email eq email) and (LoginAttemptTable.userType eq userType)

    override suspend fun recordFailedAttempt(email: String, userType: UserType, ipAddress: String?): Int = query {
        val existing = LoginAttemptDAO.find { loginAttemptPredicate(email, userType) }.singleOrNull()
        if (existing != null) {
            existing.attemptCount++
            existing.ipAddress = ipAddress
            existing.attemptCount
        } else {
            LoginAttemptDAO.new {
                this.email = email
                this.userType = userType
                this.ipAddress = ipAddress
                this.attemptCount = 1
            }
            1
        }
    }

    override suspend fun resetLoginAttempts(email: String, userType: UserType) = query {
        LoginAttemptDAO.find { loginAttemptPredicate(email, userType) }.singleOrNull()?.apply {
            attemptCount = 0; lockedUntil = null; ipAddress = null
        }
        Unit
    }

    override suspend fun getLoginAttempt(email: String, userType: UserType): LoginAttemptDAO? = query {
        LoginAttemptDAO.find { loginAttemptPredicate(email, userType) }.singleOrNull()
    }

    override suspend fun lockAccount(email: String, userType: UserType, lockDurationMinutes: Long): Boolean = query {
        LoginAttemptDAO.find { loginAttemptPredicate(email, userType) }.singleOrNull()
            ?.apply { lockedUntil = Instant.now().plusSeconds(lockDurationMinutes * 60) } != null
    }

    // ── Registration ──────────────────────────────────────────────────────

    override suspend fun register(registerRequest: RegisterRequest): RegistrationResult {
        val userTypeEnum = runCatching { UserType.valueOf(registerRequest.userType.uppercase()) }
            .getOrDefault(UserType.CUSTOMER)

        val (email, otp, result) = query {
            val existingUser =
                UserDAO.find { UserTable.email eq registerRequest.email and (UserTable.userType eq userTypeEnum) }
                    .firstOrNull()

            val otp = generateOTP()
            val otpExpiryTime = LocalDateTime.now().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES)

            if (existingUser != null) {
                if (existingUser.isVerified) throw ValidationException(Message.Auth.USER_EXISTS)
                if (existingUser.otpExpiry?.isAfter(LocalDateTime.now()) == true) {
                    throw ValidationException(Message.Auth.OTP_ALREADY_SENT)
                }
                existingUser.otpCode = otp
                existingUser.otpExpiry = otpExpiryTime
                Triple(existingUser.email, otp, RegistrationResult.OtpResent(Message.Auth.OTP_SENT))
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
                Triple(inserted.email, otp, RegistrationResult.Created(inserted.id.value, registerRequest.email, Message.Auth.OTP_SENT))
            }
        }
        return result
    }

    // ── User helpers ──────────────────────────────────────────────────────

    override suspend fun findUserByEmailAndType(email: String, userTypeEnum: UserType): UserDAO? = query {
        UserDAO.find { UserTable.email eq email and (UserTable.userType eq userTypeEnum) }.firstOrNull()
    }

    override suspend fun findUserById(userId: String): UserDAO? = query {
        UserDAO.findById(userId)
    }

    override suspend fun findResetUserByEmail(email: String, userTypeStr: String): UserDAO {
        val entities = UserDAO.find { UserTable.email eq email }.toList()
        if (entities.isEmpty()) email.throwNotFound("User")
        val type = runCatching { UserType.valueOf(userTypeStr.uppercase()) }
            .getOrElse { throw NotFoundException(Message.Auth.userNotFoundForRole(email, userTypeStr)) }
        return entities.find { it.userType == type }
            ?: throw NotFoundException(Message.Auth.userNotFoundForRole(email, userTypeStr))
    }

    // ── Password operations ───────────────────────────────────────────────

    override suspend fun changePassword(userId: String, changePassword: ChangePassword): Boolean = query {
        val userEntity = UserDAO.findById(userId) ?: throw NotFoundException(Message.Errors.NOT_FOUND)
        if (!BCrypt.verifyer().verify(changePassword.oldPassword.toCharArray(), userEntity.password).verified) return@query false
        if (changePassword.oldPassword == changePassword.newPassword) throw ValidationException(Message.Auth.PASSWORD_SAME)
        userEntity.password = BCrypt.withDefaults().hashToString(AppConstants.BCRYPT_COST, changePassword.newPassword.toCharArray())
        true
    }

    override suspend fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest) {
        query {
            val entities = UserDAO.find { UserTable.email eq forgotPasswordRequest.email }.toList()
            if (entities.isEmpty()) forgotPasswordRequest.email.throwNotFound("User")
            val type = runCatching { UserType.valueOf(forgotPasswordRequest.userType.uppercase()) }
                .getOrElse { throw NotFoundException(Message.Auth.userNotFoundForRole(forgotPasswordRequest.email, forgotPasswordRequest.userType)) }
            val user = entities.find { it.userType == type }
                ?: throw NotFoundException(Message.Auth.userNotFoundForRole(forgotPasswordRequest.email, forgotPasswordRequest.userType))
            val otp = generateOTP()
            user.resetOtpCode = otp
            user.resetOtpExpiry = LocalDateTime.now().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES)
        }
    }

    override suspend fun resetPassword(resetPasswordRequest: ResetRequest): ResetResult = query {
        val entities = UserDAO.find { UserTable.email eq resetPasswordRequest.email }.toList()
        if (entities.isEmpty()) resetPasswordRequest.email.throwNotFound("User")
        val type = runCatching { UserType.valueOf(resetPasswordRequest.userType.uppercase()) }
            .getOrElse { throw NotFoundException(Message.Auth.userNotFoundForRole(resetPasswordRequest.email, resetPasswordRequest.userType)) }
        val user = entities.find { it.userType == type }
            ?: throw NotFoundException(Message.Auth.userNotFoundForRole(resetPasswordRequest.email, resetPasswordRequest.userType))

        if (user.resetOtpExpiry?.isBefore(LocalDateTime.now()) != false)
            return@query ResetResult.InvalidOrExpiredOtp

        if (user.resetOtpCode != resetPasswordRequest.verificationCode) return@query ResetResult.InvalidOrExpiredOtp

        if (BCrypt.verifyer().verify(resetPasswordRequest.newPassword.toCharArray(), user.password).verified)
            throw ValidationException(Message.Auth.PASSWORD_SAME)

        user.password = BCrypt.withDefaults().hashToString(AppConstants.BCRYPT_COST, resetPasswordRequest.newPassword.toCharArray())
        ResetResult.Success
    }

    // ── OTP ───────────────────────────────────────────────────────────────

    override suspend fun verifyOtp(userId: String, otp: String): Boolean = query {
        val userEntity = UserDAO.findById(userId) ?: throw NotFoundException(Message.Errors.NOT_FOUND)
        if (userEntity.otpExpiry?.isBefore(LocalDateTime.now()) != false) return@query false
        val isValid = userEntity.otpCode == otp
        if (isValid) {
            userEntity.isVerified = true
            userEntity.otpCode = null
            userEntity.otpExpiry = null
        }
        isValid
    }

    override suspend fun invalidateOtp(userId: String) = query {
        val userEntity = UserDAO.findById(userId) ?: return@query
        userEntity.otpCode = null
        userEntity.otpExpiry = null
    }

    // ── Token refresh ─────────────────────────────────────────────────────

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
        currentUserId.requireNotBlank("Current User ID")
        targetUserId.requireNotBlank("Target User ID")

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
