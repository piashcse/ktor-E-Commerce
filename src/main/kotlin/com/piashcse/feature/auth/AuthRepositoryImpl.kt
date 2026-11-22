package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.RbacDefaults
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.EventBus
import com.piashcse.feature.rbac.RbacService
import com.piashcse.model.request.*
import com.piashcse.model.response.RegistrationResult
import com.piashcse.model.response.ResetResult
import com.piashcse.service.CacheService
import com.piashcse.utils.common.constantTimeEquals
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.UnauthorizedException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.time.Instant
import java.util.*

@Suppress("TooManyFunctions")
class AuthRepositoryImpl : AuthRepository {
    private val passwords = PasswordService()

    private val registration = RegistrationService()

    private val otpAttempts = OtpAttemptService()

    private val loginAttempts = LoginAttemptService()

    private val tokens = AuthTokenService()

    private fun userTypeOrNull(raw: String): UserType? = runCatching { UserType.valueOf(raw.uppercase()) }.getOrNull()

    private fun normalizeEmail(value: String): String = value.trim().lowercase()

    override suspend fun generateTokenPair(
        userId: String,
        email: String,
        userType: String,
    ): TokenPair = tokens.generateTokenPair(userId, email, userType)

    override suspend fun storeRefreshToken(
        userId: String,
        refreshToken: String,
    ) = tokens.storeRefreshToken(userId, refreshToken)

    override suspend fun getRefreshTokenByHash(tokenHash: String): RefreshTokenDAO? = tokens.getRefreshTokenByHash(tokenHash)

    override suspend fun revokeRefreshToken(tokenHash: String): Boolean = tokens.revokeRefreshToken(tokenHash)

    override suspend fun revokeAllUserTokens(userId: String): Boolean = tokens.revokeAllUserTokens(userId)

    // ── Login attempt helpers ────────────────────────────────────────────

    override suspend fun recordFailedAttempt(
        email: String,
        userType: UserType,
        ipAddress: String?,
    ): Int = loginAttempts.recordFailedAttempt(email, userType, ipAddress)

    override suspend fun resetLoginAttempts(
        email: String,
        userType: UserType,
        ipAddress: String?,
    ) = loginAttempts.resetLoginAttempts(email, userType, ipAddress)

    override suspend fun getLoginAttempt(
        email: String,
        userType: UserType,
        ipAddress: String?,
    ): LoginAttemptDAO? = loginAttempts.getLoginAttempt(email, userType, ipAddress)

    override suspend fun lockAccount(
        email: String,
        userType: UserType,
        lockDurationMinutes: Long,
        ipAddress: String?,
    ): Boolean = loginAttempts.lockAccount(email, userType, lockDurationMinutes, ipAddress)

    // ── Registration ──────────────────────────────────────────────────────

    override suspend fun register(registerRequest: RegisterRequest): RegistrationResult = registration.register(registerRequest)

    // ── User helpers ──────────────────────────────────────────────────────

    override suspend fun findUserByEmailAndType(
        email: String,
        userTypeEnum: UserType,
    ): UserDAO? =
        query {
            UserDAO.find { UserTable.email eq normalizeEmail(email) and (UserTable.userType eq userTypeEnum) }.firstOrNull()
        }

    override suspend fun findUserById(userId: String): UserDAO? =
        query {
            UserDAO.findById(userId)
        }

    override suspend fun recordMfaStep(
        userId: String,
        step: Long,
    ) = query {
        UserDAO.findById(userId)?.let { user ->
            val lastStep = user.mfaLastUsedStep
            if (lastStep == null || step > lastStep) user.mfaLastUsedStep = step
        }
        Unit
    }

    override suspend fun verifyUserPassword(
        userId: String,
        rawPassword: String,
    ): Boolean =
        query {
            val user = UserDAO.findById(userId) ?: return@query false
            PasswordHasher.verify(rawPassword, user.password)
        }

    override suspend fun getRegistrationOtp(userId: String): String? =
        query {
            UserDAO.findById(userId)?.otpCode
        }

    override suspend fun findResetUserByEmail(
        email: String,
        userTypeStr: String,
    ): UserDAO {
        val entities = UserDAO.find { UserTable.email eq normalizeEmail(email) }.toList()
        if (entities.isEmpty()) email.throwNotFound("User")
        val type =
            userTypeOrNull(userTypeStr)
                ?: throw NotFoundException(Message.Auth.userNotFoundForRole(userTypeStr))
        return entities.find { it.userType == type }
            ?: throw NotFoundException(Message.Auth.userNotFoundForRole(userTypeStr))
    }

    // ── Password operations ───────────────────────────────────────────────

    override suspend fun changePassword(
        userId: String,
        changePassword: ChangePassword,
    ): Boolean = passwords.changePassword(userId, changePassword)

    override suspend fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest): String =
        passwords.forgotPassword(forgotPasswordRequest)

    override suspend fun resetPassword(resetPasswordRequest: ResetRequest): ResetResult = passwords.resetPassword(resetPasswordRequest)

    override suspend fun verifyOtp(
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

    override suspend fun invalidateOtp(userId: String) = otpAttempts.invalidateOtp(userId)

    override suspend fun getOtpAttempt(userId: String): Int = otpAttempts.getOtpAttempt(userId)

    override suspend fun isOtpLocked(userId: String): Boolean = otpAttempts.isOtpLocked(userId)

    override suspend fun recordFailedOtpAttempt(userId: String): Int = otpAttempts.recordFailedOtpAttempt(userId)

    override suspend fun resetOtpAttempts(userId: String) = otpAttempts.resetOtpAttempts(userId)

    override suspend fun lockOtpAttempts(userId: String) = otpAttempts.lockOtpAttempts(userId)

    // ── Token refresh ─────────────────────────────────────────────────────

    override suspend fun refreshAccessToken(request: RefreshTokenRequest): TokenPair =
        query {
            val tokenHash = tokens.hashRefreshToken(request.refreshToken)
            val storedToken =
                RefreshTokenDAO.find { RefreshTokenTable.tokenHash eq tokenHash }.forUpdate().singleOrNull()
                    ?: throw UnauthorizedException(Message.Auth.INVALID_REFRESH_TOKEN)

            // A normally expired token is stale, not evidence of theft. Only a
            // revoked token indicates replay/reuse and should kill the family.
            if (storedToken.isExpired) {
                throw UnauthorizedException(Message.Auth.INVALID_REFRESH_TOKEN)
            }
            if (storedToken.isRevoked) {
                UserDAO.findById(storedToken.userId.value)?.tokenVersion += 1
                runCatching { tokens.revokeAllUserTokensTx(storedToken.userId.value) }
                throw UnauthorizedException(Message.Auth.TOKEN_EXPIRED)
            }

            val user = UserDAO.findById(storedToken.userId.value) ?: throw NotFoundException(Message.Errors.NOT_FOUND)
            if (!user.isActive) throw ValidationException(Message.Auth.ACCOUNT_DEACTIVATED)
            if (!user.isVerified) throw ValidationException(Message.Auth.ACCOUNT_NOT_VERIFIED)

            // Rotate old + persist replacement in the same transaction so a
            // concurrent password reset cannot leave a freshly minted token live.
            storedToken.revokedAt = Instant.now()
            val newTokenPair = tokens.newTokenPair(user.id.value, user.email, user.userType.name, user.tokenVersion)
            tokens.storeRefreshTokenTx(user.id.value, newTokenPair.refreshToken)
            newTokenPair
        }

    // ── Logout / Blacklist ────────────────────────────────────────────────

    override suspend fun logout(
        userId: String,
        refreshToken: String?,
    ): Boolean {
        if (!refreshToken.isNullOrBlank()) {
            revokeRefreshToken(tokens.hashRefreshToken(refreshToken))
        } else {
            // Logging out a browser session must not deactivate machine API
            // keys; those are revoked by password/role changes or explicitly.
            query {
                RefreshTokenDAO.find { RefreshTokenTable.userId eq userId.entityID(UserTable) }
                    .forEach { it.revokedAt = Instant.now() }
            }
        }
        return true
    }

    override suspend fun blacklistToken(token: String): Boolean {
        val result =
            query {
                if (BlacklistedTokenDAO.find { BlacklistedTokenTable.token eq token }.firstOrNull() == null) {
                    BlacklistedTokenDAO.new {
                        this.token = token
                        this.blacklistedAt = Instant.now()
                    }
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
    ): T =
        query {
            currentUserId.requireNotBlank("Current User ID")
            targetUserId.requireNotBlank("Target User ID")

            val currentUser = UserDAO.findById(currentUserId) ?: throw NotFoundException(Message.Errors.NOT_FOUND)
            val targetUser = UserDAO.findById(targetUserId) ?: throw NotFoundException(Message.Errors.NOT_FOUND)

            if (!currentUser.userType.canManage(targetUser.userType)) {
                throw ValidationException(Message.Auth.insufficientPermissions(action))
            }
            block(currentUser, targetUser)
        }

    override suspend fun changeUserType(
        currentUserId: String,
        targetUserId: String,
        newUserType: UserType,
    ): Boolean {
        val defaultRole =
            when (newUserType) {
                UserType.ADMIN -> RbacDefaults.ADMIN_ROLE_ID
                UserType.SUPER_ADMIN -> RbacDefaults.SUPER_ADMIN_ROLE_ID
                else -> null
            }
        val rbac = RbacService()
        if (defaultRole != null && !rbac.canGrantRole(currentUserId, defaultRole)) {
            throw ValidationException(Message.Auth.insufficientPermissions("grant $newUserType"))
        }
        val actor =
            withUsers(currentUserId, targetUserId, "change user type to $newUserType") { currentUser, targetUser ->
                if (currentUser.id == targetUser.id) {
                    throw ValidationException(Message.Auth.insufficientPermissions("change your own user type"))
                }
                if (!currentUser.userType.canManage(newUserType)) {
                    throw ValidationException(Message.Auth.insufficientPermissions("change user type to $newUserType"))
                }
                targetUser.userType = newUserType
                if (newUserType == UserType.SELLER && SellerDAO.find { SellerTable.userId eq targetUser.id }.firstOrNull() == null) {
                    SellerDAO.new {
                        userId = targetUser.id
                        status = ShopStatus.PENDING
                    }
                }
                Triple(currentUser.id.value, currentUser.email, currentUser.userType.name)
            }
        EventBus.publishAdminAction(actor, "USER_CHANGE_TYPE", "USER", targetUserId, "Changed to $newUserType")
        // Keep the RBAC assignment in sync with the account type. Clearing is
        // allowed for non-admin targets; assigning is restricted to ADMIN.
        rbac.setUserRoles(currentUserId, targetUserId, listOfNotNull(defaultRole))
        return true
    }

    override suspend fun deactivateUser(
        currentUserId: String,
        targetUserId: String,
    ): Boolean {
        if (currentUserId == targetUserId) {
            throw ValidationException("You cannot deactivate your own account")
        }
        val actor =
            withUsers(currentUserId, targetUserId, "deactivate user") { currentUser, targetUser ->
                if (targetUser.userType == UserType.SUPER_ADMIN) {
                    val activeSuperAdmins =
                        UserDAO.find {
                            (UserTable.userType eq UserType.SUPER_ADMIN) and (UserTable.isActive eq true)
                        }.count()
                    if (activeSuperAdmins <= 1) {
                        throw ValidationException("Cannot deactivate the last active super admin")
                    }
                }
                targetUser.isActive = false
                tokens.revokeAllUserTokensTx(targetUser.id.value)
                Triple(currentUser.id.value, currentUser.email, currentUser.userType.name)
            }
        EventBus.publishAdminAction(actor, "USER_DEACTIVATE", "USER", targetUserId)
        return true
    }

    override suspend fun activateUser(
        currentUserId: String,
        targetUserId: String,
    ): Boolean {
        val actor =
            withUsers(currentUserId, targetUserId, "activate user") { currentUser, targetUser ->
                targetUser.isActive = true
                Triple(currentUser.id.value, currentUser.email, currentUser.userType.name)
            }
        EventBus.publishAdminAction(actor, "USER_ACTIVATE", "USER", targetUserId)
        return true
    }
}
