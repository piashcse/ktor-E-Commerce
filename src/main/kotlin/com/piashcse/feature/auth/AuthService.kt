package com.piashcse.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.database.entities.*
import com.piashcse.model.request.ForgetPasswordRequest
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.model.request.LoginRequest
import com.piashcse.model.request.RefreshTokenRequest
import com.piashcse.model.request.RegisterRequest
import com.piashcse.model.request.ResetRequest
import com.piashcse.model.response.Registration
import com.piashcse.utils.*
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AuthService : AuthRepository {
    /**
     * Registers a new user with the given [registerRequest].
     * Throws an exception if the user already exists.
     *
     * @param registerRequest The request containing user details.
     * @return The response containing the registered user ID and email.
     */
    override suspend fun register(registerRequest: RegisterRequest): Any = query {
        // Check if user exists with the same email and userType
        val userEntity =
            UserDAO.find { UserTable.email eq registerRequest.email and (UserTable.userType eq registerRequest.userType) }
                .toList().singleOrNull()

        // Check if user exists with the same email but different userType
        val existingUserWithDifferentType =
            UserDAO.find { UserTable.email eq registerRequest.email and (UserTable.userType neq registerRequest.userType) }
                .toList().singleOrNull()

        val otp = generateOTP()
        val now =
            LocalDateTime.now().plusHours(24).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) // 24 hours opt expire time

        if (userEntity != null) {
            // User exists with the same email and userType
            // Check if the user is already verified
            if (userEntity.isVerified) {
                Message.USER_ALREADY_EXIST_WITH_THIS_EMAIL
            } else {
                val expiryTime = LocalDateTime.parse(userEntity.otpExpiry, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                if (expiryTime < LocalDateTime.now()) {
                    userEntity.otpCode = otp
                    sendEmail(userEntity.email, otp)
                    "${Message.NEW_OTP_SENT_TO} ${userEntity.email}"
                } else {
                    Message.OTP_ALREADY_SENT_WAIT_UNTIL_EXPIRY
                }
            }
        } else {
            // Create new user
            val inserted = UserDAO.new {
                email = registerRequest.email
                otpCode = otp
                otpExpiry = now
                password = BCrypt.withDefaults().hashToString(12, registerRequest.password.toCharArray())
                userType = registerRequest.userType
            }

            // If this is a new user (not existing with different role), create profile
            if (existingUserWithDifferentType == null) {
                UserProfileDAO.new {
                    userId = inserted.id
                }
            }

            // Send OTP
            sendEmail(inserted.email, otp)

            // Return appropriate message
            if (existingUserWithDifferentType != null) {
                Registration(
                    inserted.id.value,
                    registerRequest.email,
                    message = "${Message.OTP_SENT_TO} ${inserted.email}. You already have an account as ${existingUserWithDifferentType.userType}."
                )
            } else {
                Registration(
                    inserted.id.value, registerRequest.email, message = "${Message.OTP_SENT_TO} ${inserted.email}"
                )
            }
        }
    }

    /**
     * Logs in a user with the given [loginRequest].
     * Throws an exception if the user does not exist or the password is incorrect.
     *
     * @param loginRequest The request containing login credentials.
     * @return The response containing the authentication token.
     */
    override suspend fun login(loginRequest: LoginRequest): LoginResponse = query {
        val userEntity =
            UserDAO.find { UserTable.email eq loginRequest.email and (UserTable.userType eq loginRequest.userType) }
                .toList().singleOrNull()
        userEntity?.let {
            if (BCrypt.verifyer().verify(
                    loginRequest.password.toCharArray(), it.password
                ).verified
            ) {
                if (it.isVerified) {
                    // First, deactivate any existing refresh tokens for this user
                    RefreshTokenDAO.find { 
                        (RefreshTokenTable.userId eq it.id.value) and (RefreshTokenTable.isActive eq true) 
                    }.forEach { token ->
                        token.isActive = false
                    }
                    
                    // Generate a new refresh token
                    val refreshTokenValue = generateRefreshToken()
                    
                    // Store the refresh token in the database with client information
                    RefreshTokenDAO.new {
                        token = refreshTokenValue
                        userId = it.id.value
                        expiryDate = (System.currentTimeMillis() + JwtConfig.REFRESH_TOKEN_VALIDITY_MS) // 7 days validity in milliseconds
                        // createdDate will be set automatically by default
                        isActive = true
                        userAgent = loginRequest.userAgent // Store user agent if provided
                        ipAddress = loginRequest.ipAddress // Store IP address if provided
                    }
                    
                    it.loggedInWithToken(refreshTokenValue)
                } else {
                    throw CommonException(Message.ACCOUNT_NOT_VERIFIED)
                }
            } else {
                throw PasswordNotMatch()
            }
        } ?: throw loginRequest.email.notFoundException()
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
        } ?: throw UserNotExistException()
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
                    throw CommonException(Message.NEW_PASSWORD_CANNOT_BE_SAME_AS_OLD_PASSWORD)
                }
                it.password = BCrypt.withDefaults().hashToString(12, changePassword.newPassword.toCharArray())
                true
            } else {
                false
            }
        } ?: throw UserNotExistException()
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
            throw forgetPasswordRequest.email.notFoundException()
        }

        // Find the specific user with the given email and userType
        val specificUser = userEntities.find { it.userType == forgetPasswordRequest.userType }
        specificUser?.let {
            val otp = generateOTP()
            it.otpCode = otp
            otp
        }
            ?: throw "${forgetPasswordRequest.email} not found for ${forgetPasswordRequest.userType} role".notFoundException()
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
            throw resetPasswordRequest.email.notFoundException()
        }

        // Find the specific user with the given email and userType
        val userEntity = userEntities.find { it.userType == resetPasswordRequest.userType }
            ?: throw "${resetPasswordRequest.email} not found for ${resetPasswordRequest.userType} role".notFoundException()

        // Verify the code and update the password
        if (userEntity.otpCode == resetPasswordRequest.verificationCode) {
            // Check if new password is same as current password
            if (BCrypt.verifyer()
                    .verify(resetPasswordRequest.newPassword.toCharArray(), userEntity.password).verified
            ) {
                throw CommonException(Message.NEW_PASSWORD_CANNOT_BE_SAME_AS_CURRENT_PASSWORD)
            }
            userEntity.password = BCrypt.withDefaults().hashToString(12, resetPasswordRequest.newPassword.toCharArray())
            AppConstants.DataBaseTransaction.FOUND
        } else {
            AppConstants.DataBaseTransaction.NOT_FOUND
        }
    }

    override suspend fun refreshToken(refreshTokenRequest: RefreshTokenRequest): LoginResponse = query {
        // Find the refresh token in the database
        val refreshTokenEntity = RefreshTokenDAO.find { 
            RefreshTokenTable.token eq refreshTokenRequest.refreshToken 
        }.singleOrNull()

        // Check if the token exists and is still active and not expired
        if (refreshTokenEntity != null && 
            refreshTokenEntity.isActive && 
            refreshTokenEntity.expiryDate > System.currentTimeMillis()) {
            
            // Find the associated user
            val userEntity = UserDAO.findById(refreshTokenEntity.userId)
            userEntity?.let {
                if (it.isVerified) {
                    // Check if we're within the maximum lifetime for this token
                    // We'll implement a sliding window approach - if the token was created more than 
                    // MAX_REFRESH_TOKEN_LIFETIME_MS ago, we'll reject it
                    val maxLifetimeExpiry = refreshTokenEntity.createdDate + JwtConfig.MAX_REFRESH_TOKEN_LIFETIME_MS
                    
                    if (System.currentTimeMillis() > maxLifetimeExpiry) {
                        // Token has exceeded its maximum lifetime, deactivate it
                        refreshTokenEntity.isActive = false
                        throw CommonException("Refresh token has exceeded maximum lifetime")
                    }
                    
                    // Optional: Add additional security checks such as user agent/IP matching
                    // This is commented out for now as these checks might be too restrictive for mobile apps or changing networks
                    /*
                    val currentClientInfo = extractClientInfo() // Function to extract from the request
                    if (refreshTokenEntity.userAgent != null && refreshTokenEntity.userAgent != currentClientInfo.userAgent) {
                        // User agent mismatch - potential security issue
                        refreshTokenEntity.isActive = false
                        throw CommonException("Security mismatch - user agent changed")
                    }
                    if (refreshTokenEntity.ipAddress != null && refreshTokenEntity.ipAddress != currentClientInfo.ipAddress) {
                        // IP address mismatch - potential security issue
                        refreshTokenEntity.isActive = false
                        throw CommonException("Security mismatch - IP address changed")
                    }
                    */
                    
                    // Generate new tokens
                    val newRefreshTokenValue = generateRefreshToken()
                    
                    // Deactivate the old refresh token to prevent reuse (rotation)
                    refreshTokenEntity.isActive = false
                    
                    // Store the new refresh token in the database with updated expiry
                    RefreshTokenDAO.new {
                        token = newRefreshTokenValue
                        userId = it.id.value
                        expiryDate = (System.currentTimeMillis() + JwtConfig.REFRESH_TOKEN_VALIDITY_MS) // 7 days validity
                        // createdDate will be set automatically by default (this is a new token)
                        isActive = true
                        userAgent = refreshTokenEntity.userAgent // Copy original user agent for validation
                        ipAddress = refreshTokenEntity.ipAddress // Copy original IP for validation
                    }
                    
                    // Return new access token and refresh token with metadata
                    LoginResponse(
                        user = userEntity.response(),
                        accessToken = JwtConfig.tokenProvider(JwtTokenRequest(it.id.value, it.email, it.userType)),
                        refreshToken = newRefreshTokenValue,
                        tokenType = "Bearer",
                        expiresIn = JwtConfig.ACCESS_TOKEN_VALIDITY_MS / 1000 // 30 minutes in seconds
                    )
                } else {
                    throw CommonException(Message.ACCOUNT_NOT_VERIFIED)
                }
            } ?: throw "User not found for refresh token".notFoundException()
        } else {
            // Token is invalid, expired, or has been used already (not active)
            // Mark as invalid if it exists but is not active (possible reuse attack)
            refreshTokenEntity?.let {
                if (!it.isActive) {
                    // This token has already been used - possible security issue
                    // Log security event if needed
                }
            }
            throw CommonException("Invalid or expired refresh token")
        }
    }

    override suspend fun logout(userId: String, refreshToken: String?): Boolean = query {
        var tokensUpdated = false
        
        // If refresh token is provided, deactivate only that specific token
        if (!refreshToken.isNullOrEmpty()) {
            val refreshTokenEntity = RefreshTokenDAO.find { 
                RefreshTokenTable.token eq refreshToken 
            }.singleOrNull()
            
            refreshTokenEntity?.let {
                it.isActive = false
                tokensUpdated = true
            }
        }
        
        // Deactivate all active refresh tokens for the user
        RefreshTokenDAO.find { 
            (RefreshTokenTable.userId eq userId) and (RefreshTokenTable.isActive eq true) 
        }.forEach { token ->
            token.isActive = false
            tokensUpdated = true
        }
        
        tokensUpdated
    }
}