package com.piashcse.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.model.request.ForgetPasswordRequest
import com.piashcse.model.request.LoginRequest
import com.piashcse.model.request.RegisterRequest
import com.piashcse.model.request.ResetRequest
import com.piashcse.model.response.Registration
import com.piashcse.utils.*
import com.piashcse.utils.ValidationException
import com.piashcse.utils.ValidationUtils
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import java.time.LocalDateTime

class AuthService : AuthRepository {
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
                throw CommonException(Message.USER_ALREADY_EXIST_WITH_THIS_EMAIL)
            } else {
                // Resend OTP if expired
                if (existingUserSameType.otpExpiry!! < LocalDateTime.now()) {
                    existingUserSameType.otpCode = otp
                    sendEmail(existingUserSameType.email, otp)
                    "${Message.NEW_OTP_SENT_TO} ${existingUserSameType.email}"
                } else {
                    throw CommonException(Message.OTP_ALREADY_SENT_WAIT_UNTIL_EXPIRY)
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
        if (!ValidationUtils.validateEmail(request.email))
            throw ValidationException("Invalid email format")
        if (!ValidationUtils.validatePassword(request.password))
            throw ValidationException("Password must be at least 8 characters with at least one letter and one number")
        if (request.userType !in listOf("CUSTOMER", "SELLER", "ADMIN", "SUPER_ADMIN"))
            throw ValidationException("Invalid user type. Must be one of: CUSTOMER, SELLER, ADMIN, SUPER_ADMIN")
    }

    /**
     * Logs in a user with the given [loginRequest].
     * Throws an exception if the user does not exist or the password is incorrect.
     *
     * @param loginRequest The request containing login credentials.
     * @return The response containing the authentication token.
     */
    override suspend fun login(loginRequest: LoginRequest): LoginResponse = query {
        // Validate login request
        validateLoginRequest(loginRequest)

        // Convert string userType to enum for comparison
        val userTypeEnum = try {
            UserType.valueOf(loginRequest.userType.uppercase())
        } catch (e: IllegalArgumentException) {
            // If the value is not valid, return not found
            throw loginRequest.email.notFoundException()
        }

        val userEntity =
            UserDAO.find { UserTable.email eq loginRequest.email and (UserTable.userType eq userTypeEnum) }
                .toList().singleOrNull()
        userEntity?.let {
            if (BCrypt.verifyer().verify(
                    loginRequest.password.toCharArray(), it.password
                ).verified
            ) {
                if (it.isVerified) {
                    if (it.isActive) {
                        it.loggedInWithToken()
                    } else {
                        throw CommonException(Message.ACCOUNT_DEACTIVATED)
                    }
                } else {
                    throw CommonException(Message.ACCOUNT_NOT_VERIFIED)
                }
            } else {
                throw PasswordNotMatch()
            }
        } ?: throw loginRequest.email.notFoundException()
    }

    private fun validateLoginRequest(request: LoginRequest) {
        if (!ValidationUtils.validateEmail(request.email))
            throw ValidationException("Invalid email format")
        if (request.userType !in listOf("CUSTOMER", "SELLER", "ADMIN", "SUPER_ADMIN"))
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

        // Convert string userType to enum for comparison
        val userTypeEnum = try {
            UserType.valueOf(forgetPasswordRequest.userType.uppercase())
        } catch (e: IllegalArgumentException) {
            throw forgetPasswordRequest.email.notFoundException()
        }

        // Find the specific user with the given email and userType
        val specificUser = userEntities.find { it.userType == userTypeEnum }
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

        // Convert string userType to enum for comparison
        val userTypeEnum = try {
            UserType.valueOf(resetPasswordRequest.userType.uppercase())
        } catch (e: IllegalArgumentException) {
            throw "${resetPasswordRequest.email} not found for ${resetPasswordRequest.userType} role".notFoundException()
        }

        // Find the specific user with the given email and userType
        val userEntity = userEntities.find { it.userType == userTypeEnum }
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

    /**
     * Changes the user type for a specific user.
     * Only admins and super admins can change user types, with proper validation.
     */
    override suspend fun changeUserType(currentUserId: String, targetUserId: String, newUserType: UserType): Boolean = query {
        // Validate inputs
        if (currentUserId.isBlank()) throw ValidationException("Current user ID cannot be blank")
        if (targetUserId.isBlank()) throw ValidationException("Target user ID cannot be blank")

        // Get the current user making the change
        val currentUser = UserDAO.findById(currentUserId) ?: throw UserNotExistException()

        // Get the target user
        val targetUser = UserDAO.findById(targetUserId) ?: throw UserNotExistException()

        // Check if the current user has permission to change user types
        if (!RoleHierarchy.canManageUser(currentUser.userType, targetUser.userType)) {
            throw CommonException("Insufficient permissions to change user type to $newUserType")
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
            ?: throw UserNotExistException()

        // Get the target user
        val targetUser = UserDAO.find { UserTable.id eq targetUserId }.singleOrNull()
            ?: throw UserNotExistException()

        // Check if the current user has permission to deactivate this user
        if (!RoleHierarchy.canManageUser(currentUser.userType, targetUser.userType)) {
            throw CommonException("Insufficient permissions to deactivate user")
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
            ?: throw UserNotExistException()

        // Get the target user
        val targetUser = UserDAO.find { UserTable.id eq targetUserId }.singleOrNull()
            ?: throw UserNotExistException()

        // Check if the current user has permission to activate this user
        if (!RoleHierarchy.canManageUser(currentUser.userType, targetUser.userType)) {
            throw CommonException("Insufficient permissions to activate user")
        }

        // Update user active status
        targetUser.isActive = true
        true
    }
}