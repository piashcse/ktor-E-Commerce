package com.piashcse.feature.auth

import com.piashcse.constants.Message
import com.piashcse.constants.RbacDefaults
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.event.EventBus
import com.piashcse.feature.rbac.RbacService
import com.piashcse.model.request.*
import com.piashcse.model.response.RegistrationResult
import com.piashcse.model.response.ResetResult
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.util.*

@Suppress("TooManyFunctions")
class AuthRepositoryImpl : AuthRepository {
    private val otpVerification = OtpVerificationService()
    private val sessions = AuthSessionService()
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
    ): Boolean = otpVerification.verifyOtp(userId, otp)

    override suspend fun invalidateOtp(userId: String) = otpAttempts.invalidateOtp(userId)

    override suspend fun getOtpAttempt(userId: String): Int = otpAttempts.getOtpAttempt(userId)

    override suspend fun isOtpLocked(userId: String): Boolean = otpAttempts.isOtpLocked(userId)

    override suspend fun recordFailedOtpAttempt(userId: String): Int = otpAttempts.recordFailedOtpAttempt(userId)

    override suspend fun resetOtpAttempts(userId: String) = otpAttempts.resetOtpAttempts(userId)

    override suspend fun lockOtpAttempts(userId: String) = otpAttempts.lockOtpAttempts(userId)

    override suspend fun refreshAccessToken(request: RefreshTokenRequest): TokenPair = sessions.refreshAccessToken(request)

    override suspend fun logout(
        userId: String,
        refreshToken: String?,
    ): Boolean = sessions.logout(userId, refreshToken)

    override suspend fun blacklistToken(token: String): Boolean = sessions.blacklistToken(token)

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
