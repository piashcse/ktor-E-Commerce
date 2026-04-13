package com.piashcse.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.*
import com.piashcse.model.request.ForgetPasswordRequest
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.model.request.LoginRequest
import com.piashcse.model.request.RefreshTokenRequest
import com.piashcse.model.request.RegisterRequest
import com.piashcse.model.request.ResetRequest
import com.piashcse.model.request.TokenPair
import com.piashcse.model.response.Registration
import com.piashcse.utils.RoleHierarchy
import com.piashcse.utils.ValidationException
import com.piashcse.utils.InvalidCredentialsException
import com.piashcse.utils.NotFoundException
import com.piashcse.utils.requireValidEmail
import com.piashcse.utils.requireValidPassword
import com.piashcse.utils.throwNotFound
import com.piashcse.utils.extension.query
import com.piashcse.utils.generateOTP
import com.piashcse.utils.sendEmail
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class AuthService(
    private val refreshTokenRepository: RefreshTokenRepository = RefreshTokenRepositoryImpl()
) : AuthRepository {

    companion object {
        private const val REFRESH_TOKEN_EXPIRY_SECONDS = 7L * 24 * 60 * 60 // 7 days
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
            throw ValidationException("Password must be at least 8 characters long")
        }
        if (!password.any { it.isUpperCase() }) {
            throw ValidationException("Password must contain at least one uppercase letter")
        }
        if (!password.any { it.isLowerCase() }) {
            throw ValidationException("Password must contain at least one lowercase letter")
        }
        if (!password.any { it.isDigit() }) {
            throw ValidationException("Password must contain at least one digit")
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            throw ValidationException("Password must contain at least one special character")
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
                throw ValidationException(Message.USER_ALREADY_EXIST_WITH_THIS_EMAIL)
            } else {
                // Resend OTP if expired
                if (existingUserSameType.otpExpiry!! < LocalDateTime.now()) {
                    existingUserSameType.otpCode = otp
                    sendEmail(existingUserSameType.email, otp)
                    "${Message.NEW_OTP_SENT_TO} ${existingUserSameType.email}"
                } else {
                    throw ValidationException(Message.OTP_ALREADY_SENT_WAIT_UNTIL_EXPIRY)
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

            // Return appropriate message
            val messageSuffix = if (existingUserDifferentType != null) {
                ". You already have an account as ${existingUserDifferentType.userType}."
            } else {
                ""
            }

            Registration(
                inserted.id.value,
                registerRequest.email,
                message = "${Message.OTP_SENT_TO} ${inserted.email}$messageSuffix"
            )
        }
    }

    private fun validateRegisterRequest(request: RegisterRequest) {
        requireValidEmail(request.email)
        validatePasswordStrength(request.password)
        if (UserType.fromString(request.userType) == null)
            throw ValidationException("Invalid user type. Must be one of: CUSTOMER, SELLER, ADMIN, SUPER_ADMIN")
    }

    /**
     * Logs in a user with the given [loginRequest].
     * Throws an exception if the user does not exist or the password is incorrect.
     *
     * @param loginRequest The request containing login credentials.
     * @return The response containing the user info and tokens.
     */
    override suspend fun login(loginRequest: LoginRequest): LoginResponse {
        // Validate login request
        validateLoginRequest(loginRequest)

        // Convert string userType to enum for comparison
        val userTypeEnum = UserType.fromString(loginRequest.userType) ?: run {
            throw loginRequest.email.throwNotFound("Resource")
        }

        val userEntity = query {
            UserDAO.find { UserTable.email eq loginRequest.email and (UserTable.userType eq userTypeEnum) }
                .toList().singleOrNull()
        }

        val user = userEntity ?: throw loginRequest.email.throwNotFound("Resource")

        if (BCrypt.verifyer().verify(
                loginRequest.password.toCharArray(), user.password
            ).verified
        ) {
            if (user.isVerified) {
                if (user.isActive) {
                    val tokenPair = generateTokenPair(user.id.value, user.email, user.userType.name)
                    storeRefreshToken(user.id.value, tokenPair.refreshToken)
                    return LoginResponse(user.response(), tokenPair.accessToken, tokenPair.refreshToken, tokenPair.expiresIn)
                } else {
                    throw ValidationException(Message.ACCOUNT_DEACTIVATED)
                }
            } else {
                throw ValidationException(Message.ACCOUNT_NOT_VERIFIED)
            }
        } else {
            throw InvalidCredentialsException()
        }
    }

    private fun validateLoginRequest(request: LoginRequest) {
        requireValidEmail(request.email)
        if (UserType.fromString(request.userType) == null)
            throw ValidationException("Invalid user type. Must be one of: CUSTOMER, SELLER, ADMIN, SUPER_ADMIN")
        if (request.password.isBlank())
            throw ValidationException("Password cannot be empty")
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
        } ?: throw NotFoundException("User not found", "USER_NOT_FOUND")
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
                    throw ValidationException(Message.NEW_PASSWORD_CANNOT_BE_SAME_AS_OLD_PASSWORD)
                }
                it.password = BCrypt.withDefaults().hashToString(12, changePassword.newPassword.toCharArray())
                true
            } else {
                false
            }
        } ?: throw NotFoundException("User not found", "USER_NOT_FOUND")
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
            throw forgetPasswordRequest.email.throwNotFound("Resource")
        }

        // Convert string userType to enum for comparison
        val userTypeEnum = try {
            UserType.valueOf(forgetPasswordRequest.userType.uppercase())
        } catch (e: IllegalArgumentException) {
            throw forgetPasswordRequest.email.throwNotFound("Resource")
        }

        // Find the specific user with the given email and userType
        val specificUser = userEntities.find { it.userType == userTypeEnum }
        specificUser?.let {
            val otp = generateOTP()
            it.otpCode = otp
            otp
        }
            ?: throw "${forgetPasswordRequest.email} not found for ${forgetPasswordRequest.userType} role".throwNotFound("Resource")
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
            throw resetPasswordRequest.email.throwNotFound("Resource")
        }

        // Convert string userType to enum for comparison
        val userTypeEnum = try {
            UserType.valueOf(resetPasswordRequest.userType.uppercase())
        } catch (e: IllegalArgumentException) {
            throw "${resetPasswordRequest.email} not found for ${resetPasswordRequest.userType} role".throwNotFound("Resource")
        }

        // Find the specific user with the given email and userType
        val userEntity = userEntities.find { it.userType == userTypeEnum }
            ?: throw "${resetPasswordRequest.email} not found for ${resetPasswordRequest.userType} role".throwNotFound("Resource")

        // Verify the code and update the password
        if (userEntity.otpCode == resetPasswordRequest.verificationCode) {
            // Check if new password is same as current password
            if (BCrypt.verifyer()
                    .verify(resetPasswordRequest.newPassword.toCharArray(), userEntity.password).verified
            ) {
                throw ValidationException(Message.NEW_PASSWORD_CANNOT_BE_SAME_AS_CURRENT_PASSWORD)
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
        if (currentUserId.isBlank()) throw ValidationException("Current user ID cannot be blank")
        if (targetUserId.isBlank()) throw ValidationException("Target user ID cannot be blank")

        // Get the current user making the change
        val currentUser = UserDAO.findById(currentUserId) ?: throw NotFoundException("User not found", "USER_NOT_FOUND")

        // Get the target user
        val targetUser = UserDAO.findById(targetUserId) ?: throw NotFoundException("User not found", "USER_NOT_FOUND")

        // Check if the current user has permission to change user types
        if (!RoleHierarchy.canManageUser(currentUser.userType, targetUser.userType)) {
            throw ValidationException("Insufficient permissions to change user type to $newUserType")
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
            ?: throw NotFoundException("User not found", "USER_NOT_FOUND")

        // Get the target user
        val targetUser = UserDAO.find { UserTable.id eq targetUserId }.singleOrNull()
            ?: throw NotFoundException("User not found", "USER_NOT_FOUND")

        // Check if the current user has permission to deactivate this user
        if (!RoleHierarchy.canManageUser(currentUser.userType, targetUser.userType)) {
            throw ValidationException("Insufficient permissions to deactivate user")
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
            ?: throw NotFoundException("User not found", "USER_NOT_FOUND")

        // Get the target user
        val targetUser = UserDAO.find { UserTable.id eq targetUserId }.singleOrNull()
            ?: throw NotFoundException("User not found", "USER_NOT_FOUND")

        // Check if the current user has permission to activate this user
        if (!RoleHierarchy.canManageUser(currentUser.userType, targetUser.userType)) {
            throw ValidationException("Insufficient permissions to activate user")
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
            ?: throw "Invalid refresh token".throwNotFound("Resource")

        if (!storedToken.isValid) {
            refreshTokenRepository.revokeRefreshToken(tokenHash)
            throw "Refresh token expired or revoked".throwNotFound("Resource")
        }

        val user = query {
            UserDAO.findById(storedToken.userId.value)
                ?: throw "User not found".throwNotFound("Resource")
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