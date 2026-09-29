package com.piashcse.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.event.AdminActionEvent
import com.piashcse.event.EventBus
import com.piashcse.model.request.*
import com.piashcse.model.response.RegistrationResult
import com.piashcse.model.response.ResetResult
import com.piashcse.service.CacheService
import com.piashcse.utils.common.constantTimeEquals
import com.piashcse.utils.common.generateOTP
import com.piashcse.utils.extension.*
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class AuthRepositoryImpl : AuthRepository {

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
        val expiresAt = Instant.now().plusSeconds(AppConstants.Authentication.REFRESH_TOKEN_EXPIRY_SECONDS)
        query {
            RefreshTokenDAO.new {
                this.userId = userId.entityID(UserTable)
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
        revokeAllUserTokensTx(userId)
        true
    }

    private fun revokeAllUserTokensTx(userId: String) {
        RefreshTokenDAO.find { RefreshTokenTable.userId eq userId.entityID(UserTable) }
            .forEach { it.revokedAt = Instant.now() }
    }

    // ── Login attempt helpers ────────────────────────────────────────────

    private fun loginAttemptPredicate(email: String, userType: UserType) =
        (LoginAttemptTable.email eq email) and (LoginAttemptTable.userType eq userType)

    override suspend fun recordFailedAttempt(email: String, userType: UserType, ipAddress: String?): Int = query {
        val existing = LoginAttemptDAO.find { loginAttemptPredicate(email, userType) }.forUpdate().singleOrNull()
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
                Triple(existingUser.email, otp, RegistrationResult.OtpResent(existingUser.id.value, existingUser.email, Message.Auth.OTP_SENT))
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

    override suspend fun getRegistrationOtp(userId: String): String? = query {
        UserDAO.findById(userId)?.otpCode
    }

    override suspend fun findResetUserByEmail(email: String, userTypeStr: String): UserDAO {
        val entities = UserDAO.find { UserTable.email eq email }.toList()
        if (entities.isEmpty()) email.throwNotFound("User")
        val type = runCatching { UserType.valueOf(userTypeStr.uppercase()) }
            .getOrElse { throw NotFoundException(Message.Auth.userNotFoundForRole(userTypeStr)) }
        return entities.find { it.userType == type }
            ?: throw NotFoundException(Message.Auth.userNotFoundForRole(userTypeStr))
    }

    // ── Password operations ───────────────────────────────────────────────

    override suspend fun changePassword(userId: String, changePassword: ChangePassword): Boolean = query {
        val userEntity = UserDAO.findById(userId) ?: throw NotFoundException(Message.Errors.NOT_FOUND)
        if (!BCrypt.verifyer().verify(changePassword.oldPassword.toCharArray(), userEntity.password).verified) return@query false
        if (changePassword.oldPassword == changePassword.newPassword) throw ValidationException(Message.Auth.PASSWORD_SAME)
        userEntity.password = BCrypt.withDefaults().hashToString(AppConstants.BCRYPT_COST, changePassword.newPassword.toCharArray())
        true
    }

    override suspend fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest): String = query {
        val entities = UserDAO.find { UserTable.email eq forgotPasswordRequest.email }.toList()
        if (entities.isEmpty()) forgotPasswordRequest.email.throwNotFound("User")
        val type = runCatching { UserType.valueOf(forgotPasswordRequest.userType.uppercase()) }
            .getOrElse { throw NotFoundException(Message.Auth.userNotFoundForRole(forgotPasswordRequest.userType)) }
        val user = entities.find { it.userType == type }
            ?: throw NotFoundException(Message.Auth.userNotFoundForRole(forgotPasswordRequest.userType))
        val otp = generateOTP()
        user.resetOtpCode = otp
        user.resetOtpExpiry = LocalDateTime.now().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES)
        otp
    }

    override suspend fun resetPassword(resetPasswordRequest: ResetRequest): ResetResult = query {
        val entities = UserDAO.find { UserTable.email eq resetPasswordRequest.email }.toList()
        if (entities.isEmpty()) resetPasswordRequest.email.throwNotFound("User")
        val type = runCatching { UserType.valueOf(resetPasswordRequest.userType.uppercase()) }
            .getOrElse { throw NotFoundException(Message.Auth.userNotFoundForRole(resetPasswordRequest.userType)) }
        val user = entities.find { it.userType == type }
            ?: throw NotFoundException(Message.Auth.userNotFoundForRole(resetPasswordRequest.userType))

        val otpAttempt = OtpAttemptDAO.find { (OtpAttemptTable.userId eq user.id) and (OtpAttemptTable.purpose eq "RESET") }.singleOrNull()
        if (otpAttempt?.isLocked == true) return@query ResetResult.Locked

        if (user.resetOtpExpiry?.isBefore(LocalDateTime.now()) != false)
            return@query ResetResult.InvalidOrExpiredOtp

        if (!constantTimeEquals(user.resetOtpCode.orEmpty(), resetPasswordRequest.verificationCode)) {
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
                attemptRecord.lockedUntil = Instant.now().plusSeconds(AppConstants.Authentication.OTP_LOCKOUT_MINUTES * 60)
                user.resetOtpCode = null
                user.resetOtpExpiry = null
            }
            return@query ResetResult.InvalidOrExpiredOtp
        }

        if (BCrypt.verifyer().verify(resetPasswordRequest.newPassword.toCharArray(), user.password).verified)
            throw ValidationException(Message.Auth.PASSWORD_SAME)

        otpAttempt?.delete()
        user.resetOtpCode = null
        user.resetOtpExpiry = null
        user.password = BCrypt.withDefaults().hashToString(AppConstants.BCRYPT_COST, resetPasswordRequest.newPassword.toCharArray())
        ResetResult.Success
    }

    // ── OTP ───────────────────────────────────────────────────────────────

    override suspend fun verifyOtp(userId: String, otp: String): Boolean = query {
        OtpAttemptDAO.find { (OtpAttemptTable.userId eq userId.entityID(UserTable)) and (OtpAttemptTable.purpose eq "GENERAL") }.singleOrNull()?.let {
            if (it.isLocked) throw ValidationException(Message.Auth.accountLocked(AppConstants.Authentication.OTP_LOCKOUT_MINUTES))
        }
        val userEntity = UserDAO.findById(userId) ?: throw NotFoundException(Message.Errors.NOT_FOUND)
        if (userEntity.otpExpiry?.isBefore(LocalDateTime.now()) != false) return@query false
        val isValid = constantTimeEquals(userEntity.otpCode.orEmpty(), otp)
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

    // ── OTP attempt tracking (persistent) ─────────────────────────────────

    override suspend fun getOtpAttempt(userId: String): Int = query {
        OtpAttemptDAO.find { (OtpAttemptTable.userId eq userId.entityID(UserTable)) and (OtpAttemptTable.purpose eq "GENERAL") }
            .singleOrNull()?.attemptCount ?: 0
    }

    override suspend fun isOtpLocked(userId: String): Boolean = query {
        OtpAttemptDAO.find { (OtpAttemptTable.userId eq userId.entityID(UserTable)) and (OtpAttemptTable.purpose eq "GENERAL") }
            .singleOrNull()?.isLocked == true
    }

    override suspend fun recordFailedOtpAttempt(userId: String): Int = query {
        val existing = OtpAttemptDAO.find { (OtpAttemptTable.userId eq userId.entityID(UserTable)) and (OtpAttemptTable.purpose eq "GENERAL") }
            .forUpdate().singleOrNull()
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

    override suspend fun resetOtpAttempts(userId: String) {
        query {
            OtpAttemptDAO.find { (OtpAttemptTable.userId eq userId.entityID(UserTable)) and (OtpAttemptTable.purpose eq "GENERAL") }
                .singleOrNull()?.delete()
        }
    }

    override suspend fun lockOtpAttempts(userId: String) {
        query {
            OtpAttemptDAO.find { (OtpAttemptTable.userId eq userId.entityID(UserTable)) and (OtpAttemptTable.purpose eq "GENERAL") }
                .singleOrNull()?.apply {
                    lockedUntil = Instant.now().plusSeconds(AppConstants.Authentication.OTP_LOCKOUT_MINUTES * 60)
                }
        }
    }

    // ── Token refresh ─────────────────────────────────────────────────────

    override suspend fun refreshAccessToken(request: RefreshTokenRequest): TokenPair {
        val tokenHash = hashRefreshToken(request.refreshToken)
        // Single transaction with row-level lock to prevent concurrent reuse.
        val userSnapshot = query {
            val storedToken =
                RefreshTokenDAO.find { RefreshTokenTable.tokenHash eq tokenHash }.forUpdate().singleOrNull()
                    ?: throw NotFoundException(Message.Auth.INVALID_REFRESH_TOKEN)

            if (!storedToken.isValid) {
                storedToken.revokedAt = Instant.now()
                throw NotFoundException(Message.Auth.TOKEN_EXPIRED)
            }

            val user = UserDAO.findById(storedToken.userId.value) ?: throw NotFoundException(Message.Errors.NOT_FOUND)

            if (!user.isActive) throw ValidationException(Message.Auth.ACCOUNT_DEACTIVATED)
            if (!user.isVerified) throw ValidationException(Message.Auth.ACCOUNT_NOT_VERIFIED)

            storedToken.revokedAt = Instant.now()
            Triple(user.id.value, user.email, user.userType.name)
        }

        val newTokenPair = generateTokenPair(userSnapshot.first, userSnapshot.second, userSnapshot.third)
        storeRefreshToken(userSnapshot.first, newTokenPair.refreshToken)
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

    override suspend fun blacklistToken(token: String): Boolean {
        val result = query {
            if (BlacklistedTokenDAO.find { BlacklistedTokenTable.token eq token }.firstOrNull() == null) {
                BlacklistedTokenDAO.new { this.token = token; this.blacklistedAt = Instant.now() }
            }
            true
        }
        CacheService.cache.set("blacklisted_token:$token", true, AppConstants.Authentication.JWT_EXPIRY_SECONDS)
        return result
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

    override suspend fun changeUserType(currentUserId: String, targetUserId: String, newUserType: UserType): Boolean {
        val actor = withUsers(currentUserId, targetUserId, "change user type to $newUserType") { currentUser, targetUser ->
            if (currentUser.id == targetUser.id) {
                throw ValidationException(Message.Auth.insufficientPermissions("change your own user type"))
            }
            if (!currentUser.userType.canManage(newUserType)) {
                throw ValidationException(Message.Auth.insufficientPermissions("change user type to $newUserType"))
            }
            targetUser.userType = newUserType
            if (newUserType == UserType.SELLER && SellerDAO.find { SellerTable.userId eq targetUser.id }.firstOrNull() == null) {
                SellerDAO.new { userId = targetUser.id; status = ShopStatus.PENDING }
            }
            Triple(currentUser.id.value, currentUser.email, currentUser.userType.name)
        }
        EventBus.publish(AdminActionEvent(actor.first, actor.second, actor.third, "USER_CHANGE_TYPE", "USER", targetUserId, "Changed to $newUserType"))
        return true
    }

    override suspend fun deactivateUser(currentUserId: String, targetUserId: String): Boolean {
        val actor = withUsers(currentUserId, targetUserId, "deactivate user") { currentUser, targetUser ->
            targetUser.isActive = false
            revokeAllUserTokensTx(targetUser.id.value)
            Triple(currentUser.id.value, currentUser.email, currentUser.userType.name)
        }
        EventBus.publish(AdminActionEvent(actor.first, actor.second, actor.third, "USER_DEACTIVATE", "USER", targetUserId, null))
        return true
    }

    override suspend fun activateUser(currentUserId: String, targetUserId: String): Boolean {
        val actor = withUsers(currentUserId, targetUserId, "activate user") { currentUser, targetUser ->
            targetUser.isActive = true
            Triple(currentUser.id.value, currentUser.email, currentUser.userType.name)
        }
        EventBus.publish(AdminActionEvent(actor.first, actor.second, actor.third, "USER_ACTIVATE", "USER", targetUserId, null))
        return true
    }
}
