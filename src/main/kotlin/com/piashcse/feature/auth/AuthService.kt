package com.piashcse.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.model.request.*
import com.piashcse.model.response.Registration
import com.piashcse.utils.*
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class AuthService(
    private val refreshTokenRepository: RefreshTokenRepository = RefreshTokenRepositoryImpl(),
    private val loginAttemptRepository: LoginAttemptRepository = LoginAttemptRepositoryImpl()
) : AuthRepository {

    companion object {
        private const val REFRESH_TOKEN_EXPIRY_SECONDS = 7L * 24 * 60 * 60 // 7 days
        private const val MAX_LOGIN_ATTEMPTS = 5
        private const val ACCOUNT_LOCKOUT_MINUTES = 30L
    }

    private fun hashRefreshToken(token: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun generateTokenPair(userId: String, email: String, userType: String): TokenPair {
        val accessToken = JwtConfig.tokenProvider(JwtTokenRequest(userId, email, userType))
        val refreshToken = UUID.randomUUID().toString()
        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 86400
        )
    }

    private suspend fun storeRefreshToken(userId: String, refreshToken: String) {
        val tokenHash = hashRefreshToken(refreshToken)
        val expiresAt = Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRY_SECONDS)
        refreshTokenRepository.createRefreshToken(userId, tokenHash, expiresAt)
    }

    private fun validatePasswordStrength(password: String) {
        if (password.length < 8) {
            throw ValidationException(Message.Validation.WEAK_PASSWORD)
        }
        if (!password.any { it.isUpperCase() }) {
            throw ValidationException(Message.Validation.WEAK_PASSWORD)
        }
        if (!password.any { it.isLowerCase() }) {
            throw ValidationException(Message.Validation.WEAK_PASSWORD)
        }
        if (!password.any { it.isDigit() }) {
            throw ValidationException(Message.Validation.WEAK_PASSWORD)
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            throw ValidationException(Message.Validation.WEAK_PASSWORD)
        }
    }
    /**
     * Registers a new user with the given [registerRequest].
     * Throws an exception if the user already exists.
     *
     * @param registerRequest The request containing user details.
     * @return The response containing the registered user ID and email.
     */
    override suspend fun register(registerRequest: RegisterRequest): Any = query {
        // Validate request data
        validateRegisterRequest(registerRequest)

        // Convert string userType to enum for the database query
        val userTypeEnum = try {
            UserType.valueOf(registerRequest.userType.uppercase())
        } catch (e: IllegalArgumentException) {
            // If the value is not valid, default to CUSTOMER
            UserType.CUSTOMER
        }

        // Check if user exists with the same email and userType
        val existingUserSameType =
            UserDAO.find { UserTable.email eq registerRequest.email and (UserTable.userType eq userTypeEnum) }
                .singleOrNull()

        // Check if user exists with the same email but different userType
        val existingUserDifferentType =
            UserDAO.find { UserTable.email eq registerRequest.email and (UserTable.userType neq userTypeEnum) }
                .singleOrNull()

        val otp = generateOTP()
        val now = LocalDateTime.now().plusHours(24) // 24 hours otp expire time

        if (existingUserSameType != null) {
            // User exists with the same email and userType
            // Check if the user is already verified
            if (existingUserSameType.isVerified) {
                throw ValidationException(Message.Auth.USER_EXISTS)
            } else {
                // Resend OTP if expired
                if (existingUserSameType.otpExpiry!! < LocalDateTime.now()) {
                    existingUserSameType.otpCode = otp
                    sendEmail(existingUserSameType.email, otp)
                    Message.Auth.OTP_SENT
                } else {
                    throw ValidationException(Message.Auth.OTP_ALREADY_SENT)
                }
            }
        } else {
            // Create new user
            val inserted = UserDAO.new {
                email = registerRequest.email
                otpCode = otp
                otpExpiry = now
                password = BCrypt.withDefaults().hashToString(12, registerRequest.password.toCharArray())
                userType = userTypeEnum
            }

            // Create user profile
            UserProfileDAO.new {
                userId = inserted.id
            }

            // Create corresponding seller record if user is registering as a seller
            if (userTypeEnum == UserType.SELLER) {
                SellerDAO.new {
                    userId = inserted.id
                    status = ShopStatus.PENDING // Default to pending approval
                }
            }

            // Send OTP
            sendEmail(inserted.email, otp)

            Registration(
                inserted.id.value,
                registerRequest.email,
                message = Message.Auth.OTP_SENT
            )
        }
    }

    private fun validateRegisterRequest(request: RegisterRequest) {
        requireValidEmail(request.email)
        validatePasswordStrength(request.password)
        if (UserType.fromString(request.userType) == null)
            throw ValidationException(Message.Validation.INVALID_USER_TYPE)
    }

    /**
     * Logs in a user with the given [loginRequest].
     * Throws an exception if the user does not exist or the password is incorrect.
     * Tracks failed login attempts and locks the account after too many failures.
     *
     * @param loginRequest The request containing login credentials.
     * @return The response containing the user info and tokens.
     */
    override suspend fun login(loginRequest: LoginRequest): LoginResponse {
        validateLoginRequest(loginRequest)

        val userTypeEnum = UserType.fromString(loginRequest.userType)
            ?: loginRequest.email.throwNotFound("User")

        checkAccountLockout(loginRequest.email, userTypeEnum)

        val user = findUserByEmailAndType(loginRequest.email, userTypeEnum)
            ?: run {
                loginAttemptRepository.recordFailedAttempt(loginRequest.email, userTypeEnum, null)
                loginRequest.email.throwNotFound("User")
            }

        if (!isPasswordValid(loginRequest.password, user.password)) {
            handleFailedLogin(loginRequest.email, userTypeEnum)
        }

        validateUserState(user)

        // Successful login
        loginAttemptRepository.resetAttempts(loginRequest.email, userTypeEnum)
        return issueTokensAndLogin(user)
    }

    private suspend fun checkAccountLockout(email: String, userTypeEnum: UserType) {
        loginAttemptRepository.getAttempt(email, userTypeEnum)?.let { attempt ->
            if (attempt.isLocked) {
                throw ValidationException(Message.Auth.accountLocked(ACCOUNT_LOCKOUT_MINUTES))
            }
        }
    }

    private suspend fun findUserByEmailAndType(email: String, userTypeEnum: UserType): UserDAO? = query {
        UserDAO.find { UserTable.email eq email and (UserTable.userType eq userTypeEnum) }.singleOrNull()
    }

    private fun isPasswordValid(rawPassword: String, hashedPassword: String): Boolean =
        BCrypt.verifyer().verify(rawPassword.toCharArray(), hashedPassword).verified

    private suspend fun handleFailedLogin(email: String, userTypeEnum: UserType) {
        val attemptCount = loginAttemptRepository.recordFailedAttempt(email, userTypeEnum, null)
        if (attemptCount >= MAX_LOGIN_ATTEMPTS) {
            loginAttemptRepository.lockAccount(email, userTypeEnum, ACCOUNT_LOCKOUT_MINUTES)
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

    private fun validateLoginRequest(request: LoginRequest) {
        requireValidEmail(request.email)
        if (UserType.fromString(request.userType) == null)
            throw ValidationException(Message.Validation.INVALID_USER_TYPE)
        if (request.password.isBlank())
            throw ValidationException(Message.Validation.EMPTY_PASSWORD)
    }

    /**
     * Verify otp .
     * Throws an exception if the otp code not valid.
     *
     * @param otp The request containing the otp code.
     * @return Success after verify the otp.
     */
    override suspend fun otpVerification(userId: String, otp: String): Boolean = query {
        val userEntity = UserDAO.find { UserTable.id eq userId }.toList().singleOrNull()
        userEntity?.let {
            if (it.otpCode == otp) {
                it.isVerified = true
                true
            } else {
                false
            }
        } ?: throw NotFoundException(Message.Errors.NOT_FOUND)
    }

    /**
     * Changes the password for a user.
     * Throws an exception if the user does not exist or the old password is incorrect.
     *
     * @param userId The ID of the user.
     * @param changePassword The request containing the old and new passwords.
     * @return `true` if the password is changed successfully, otherwise `false`.
     */
    override suspend fun changePassword(userId: String, changePassword: ChangePassword): Boolean = query {
        val userEntity = UserDAO.find { UserTable.id eq userId }.toList().singleOrNull()
        userEntity?.let {
            if (BCrypt.verifyer().verify(changePassword.oldPassword.toCharArray(), it.password).verified) {
                // Check if new password is same as old password
                if (changePassword.oldPassword == changePassword.newPassword) {
                    throw ValidationException(Message.Auth.PASSWORD_SAME)
                }
                it.password = BCrypt.withDefaults().hashToString(12, changePassword.newPassword.toCharArray())
                true
            } else {
                false
            }
        } ?: throw NotFoundException(Message.Errors.NOT_FOUND)
    }

    /**
     * Sends a password reset code to the user.
     * Throws an exception if the user does not exist.
     *
     * @param forgetPasswordRequest The request containing the user's email.
     * @return The verification code sent to the user.
     */
    override suspend fun forgetPassword(forgetPasswordRequest: ForgetPasswordRequest): String = query {
        // Find all users with the given email
        val userEntities = UserDAO.find { UserTable.email eq forgetPasswordRequest.email }.toList()

        if (userEntities.isEmpty()) {
            forgetPasswordRequest.email.throwNotFound("User")
        }

        // Convert string userType to enum for comparison
        val userTypeEnum = try {
            UserType.valueOf(forgetPasswordRequest.userType.uppercase())
        } catch (e: IllegalArgumentException) {
            forgetPasswordRequest.email.throwNotFound("User")
        }

        // Find the specific user with the given email and userType
        val specificUser = userEntities.find { it.userType == userTypeEnum }
        specificUser?.let {
            val otp = generateOTP()
            it.otpCode = otp
            otp
        } ?: throw NotFoundException(Message.Auth.userNotFoundForRole(forgetPasswordRequest.email, forgetPasswordRequest.userType))
    }

    /**
     * Verifies the password reset code and updates the password if the code is valid.
     * If the verification code matches, the password is updated and the code is cleared.
     * Returns a constant indicating whether the operation was successful or not.
     *
     * @param resetPasswordRequest The request containing email, verification code, and new password.
     * @return `FOUND` if the verification code is correct and the password is updated, otherwise `NOT_FOUND`.
     * @throws Exception if the user does not exist.
     */
    override suspend fun resetPassword(resetPasswordRequest: ResetRequest): Int = query {
        // Find all users with the given email
        val userEntities = UserDAO.find { UserTable.email eq resetPasswordRequest.email }.toList()

        if (userEntities.isEmpty()) {
            resetPasswordRequest.email.throwNotFound("User")
        }

        // Convert string userType to enum for comparison
        val userTypeEnum = try {
            UserType.valueOf(resetPasswordRequest.userType.uppercase())
        } catch (e: IllegalArgumentException) {
            throw NotFoundException(Message.Auth.userNotFoundForRole(resetPasswordRequest.email, resetPasswordRequest.userType))
        }

        // Find the specific user with the given email and userType
        val userEntity = userEntities.find { it.userType == userTypeEnum }
            ?: throw NotFoundException(Message.Auth.userNotFoundForRole(resetPasswordRequest.email, resetPasswordRequest.userType))

        // Verify the code and update the password
        if (userEntity.otpCode == resetPasswordRequest.verificationCode) {
            // Validate new password strength
            validatePasswordStrength(resetPasswordRequest.newPassword)

            // Check if new password is same as current password
            if (BCrypt.verifyer()
                    .verify(resetPasswordRequest.newPassword.toCharArray(), userEntity.password).verified
            ) {
                throw ValidationException(Message.Auth.PASSWORD_SAME)
            }
            userEntity.password = BCrypt.withDefaults().hashToString(12, resetPasswordRequest.newPassword.toCharArray())
            AppConstants.DataBaseTransaction.FOUND
        } else {
            AppConstants.DataBaseTransaction.NOT_FOUND
        }
    }

    /**
     * Changes the user type for a specific user.
     * Only admins and super admins can change user types, with proper validation.
     */
    override suspend fun changeUserType(currentUserId: String, targetUserId: String, newUserType: UserType): Boolean = query {
        // Validate inputs
        if (currentUserId.isBlank()) throw ValidationException(Message.Validation.blankField("Current user ID"))
        if (targetUserId.isBlank()) throw ValidationException(Message.Validation.blankField("Target user ID"))

        // Get the current user making the change
        val currentUser = UserDAO.findById(currentUserId) ?: throw NotFoundException(Message.Errors.NOT_FOUND)

        // Get the target user
        val targetUser = UserDAO.findById(targetUserId) ?: throw NotFoundException(Message.Errors.NOT_FOUND)

        // Check if the current user has permission to change user types
        if (!currentUser.userType.canManage(targetUser.userType)) {
            throw ValidationException(Message.Auth.insufficientPermissions("change user type to $newUserType"))
        }

        // Update the user type
        targetUser.userType = newUserType

        // Handle seller transition
        handleSellerTransition(targetUser, newUserType)

        true
    }

    /**
     * Handles the transition when a user becomes a seller
     */
    private fun handleSellerTransition(user: UserDAO, newUserType: UserType) {
        if (newUserType == UserType.SELLER) {
            val existingSeller = SellerDAO.find { SellerTable.userId eq user.id }.singleOrNull()
            if (existingSeller == null) {
                SellerDAO.new {
                    userId = user.id
                    status = ShopStatus.PENDING // Default to pending approval
                }
            }
        }
    }

    /**
     * Deactivates a user account.
     */
    override suspend fun deactivateUser(currentUserId: String, targetUserId: String): Boolean = query {
        // Get the current user making the change
        val currentUser = UserDAO.find { UserTable.id eq currentUserId }.singleOrNull()
            ?: throw NotFoundException(Message.Errors.NOT_FOUND)

        // Get the target user
        val targetUser = UserDAO.find { UserTable.id eq targetUserId }.singleOrNull()
            ?: throw NotFoundException(Message.Errors.NOT_FOUND)

        // Check if the current user has permission to deactivate this user
        if (!currentUser.userType.canManage(targetUser.userType)) {
            throw ValidationException(Message.Auth.insufficientPermissions("deactivate user"))
        }

        // Update user active status
        targetUser.isActive = false
        true
    }

    /**
     * Activates a user account.
     */
    override suspend fun activateUser(currentUserId: String, targetUserId: String): Boolean = query {
        // Get the current user making the change
        val currentUser = UserDAO.find { UserTable.id eq currentUserId }.singleOrNull()
            ?: throw NotFoundException(Message.Errors.NOT_FOUND)

        // Get the target user
        val targetUser = UserDAO.find { UserTable.id eq targetUserId }.singleOrNull()
            ?: throw NotFoundException(Message.Errors.NOT_FOUND)

        // Check if the current user has permission to activate this user
        if (!currentUser.userType.canManage(targetUser.userType)) {
            throw ValidationException(Message.Auth.insufficientPermissions("activate user"))
        }

        // Update user active status
        targetUser.isActive = true
        true
    }

    /**
     * Refreshes an access token using a valid refresh token.
     */
    suspend fun refreshAccessToken(request: RefreshTokenRequest): TokenPair {
        request.validate()

        val tokenHash = hashRefreshToken(request.refreshToken)
        val storedToken = refreshTokenRepository.getRefreshTokenByHash(tokenHash)
            ?: throw NotFoundException(Message.Auth.INVALID_REFRESH_TOKEN)

        if (!storedToken.isValid) {
            refreshTokenRepository.revokeRefreshToken(tokenHash)
            throw NotFoundException(Message.Auth.TOKEN_EXPIRED)
        }

        val user = query {
            UserDAO.findById(storedToken.userId.value)
                ?: throw NotFoundException(Message.Errors.NOT_FOUND)
        }

        // Revoke old token and issue new pair
        refreshTokenRepository.revokeRefreshToken(tokenHash)
        return generateTokenPair(user.id.value, user.email, user.userType.name)
    }

    /**
     * Logs out a user by revoking their refresh token.
     */
    suspend fun logout(userId: String, refreshToken: String?): Boolean {
        if (!refreshToken.isNullOrBlank()) {
            val tokenHash = hashRefreshToken(refreshToken)
            refreshTokenRepository.revokeRefreshToken(tokenHash)
        } else {
            refreshTokenRepository.revokeAllUserTokens(userId)
        }
        return true
    }
}