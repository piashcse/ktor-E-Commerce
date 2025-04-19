package com.piashcse.controller

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.entities.*
import com.piashcse.models.user.body.ForgetPasswordRequest
import com.piashcse.models.user.body.LoginRequest
import com.piashcse.models.user.body.RegisterRequest
import com.piashcse.models.user.body.ResetRequest
import com.piashcse.models.user.response.RegisterResponse
import com.piashcse.repository.AuthRepo
import com.piashcse.utils.*
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.sql.and
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AuthController : AuthRepo {
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
                "User already exists with this email"
            } else {
                val expiryTime = LocalDateTime.parse(userEntity.otpExpiry, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                if (expiryTime < LocalDateTime.now()) {
                    userEntity.otpCode = otp
                    sendEmail(userEntity.email, otp)
                    "New OTP sent to ${userEntity.email}"
                } else {
                    "OTP already sent, wait until expiry"
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
                UsersProfileDAO.new {
                    userId = inserted.id
                }
            }

            // Send OTP
            sendEmail(inserted.email, otp)

            // Return appropriate message
            if (existingUserWithDifferentType != null) {
                RegisterResponse(inserted.id.value, registerRequest.email, 
                    message = "OTP sent to ${inserted.email}. You already have an account as ${existingUserWithDifferentType.userType}.")
            } else {
                RegisterResponse(inserted.id.value, registerRequest.email, 
                    message = "OTP sent to ${inserted.email}")
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
                    it.loggedInWithToken()
                } else {
                    throw CommonException("Account is not verified")
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

        // If userType is specified, find the specific user
        if (forgetPasswordRequest.userType != null) {
            val specificUser = userEntities.find { it.userType == forgetPasswordRequest.userType }
            specificUser?.let {
                val otp = generateOTP()
                it.otpCode = otp
                otp
            } ?: throw "${forgetPasswordRequest.email} not found for ${forgetPasswordRequest.userType} role".notFoundException()
        } else {
            // If userType is not specified and multiple accounts exist, use the first one
            // In a real-world scenario, you might want to handle this differently
            val user = userEntities.first()
            val otp = generateOTP()
            user.otpCode = otp
            otp
        }
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

        // If userType is specified, find the specific user
        val userEntity = if (resetPasswordRequest.userType != null) {
            userEntities.find { it.userType == resetPasswordRequest.userType }
                ?: throw "${resetPasswordRequest.email} not found for ${resetPasswordRequest.userType} role".notFoundException()
        } else {
            // If userType is not specified and multiple accounts exist, use the first one
            // In a real-world scenario, you might want to handle this differently
            userEntities.first()
        }

        // Verify the code and update the password
        if (userEntity.otpCode == resetPasswordRequest.verificationCode) {
            userEntity.password = BCrypt.withDefaults().hashToString(12, resetPasswordRequest.newPassword.toCharArray())
            AppConstants.DataBaseTransaction.FOUND
        } else {
            AppConstants.DataBaseTransaction.NOT_FOUND
        }
    }
}
