package com.piashcse.controller

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.entities.*
import com.piashcse.models.user.body.ResetRequest
import com.piashcse.models.user.body.ForgetPasswordRequest
import com.piashcse.models.user.body.LoginRequest
import com.piashcse.models.user.body.RegisterRequest
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
        val userEntity =
            UserDAO.find { UserTable.email eq registerRequest.email and (UserTable.userType eq registerRequest.userType) }
                .toList().singleOrNull()
        val otp = generateOTP()
        val now =
            LocalDateTime.now().plusHours(24).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) // 24 hours opt expire time

        if (userEntity != null) {
            val expiryTime = LocalDateTime.parse(userEntity.otpExpiry, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            if (expiryTime < LocalDateTime.now()) {
                userEntity.otpCode = otp
                sendEmail(userEntity.email, otp)
                "New OTP sent to ${userEntity.email}"
            } else {
                "OTP already sent, wait until expiry"
            }
        } else {
            val inserted = UserDAO.new {
                email = registerRequest.email
                otpCode = otp
                otpExpiry = now
                password = BCrypt.withDefaults().hashToString(12, registerRequest.password.toCharArray())
                userType = registerRequest.userType
            }
            UsersProfileDAO.new {
                userId = inserted.id
            }
            sendEmail(inserted.email, otp)
            RegisterResponse(inserted.id.value, registerRequest.email, message = "OTP sent to ${inserted.email}")
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
        val userEntity = UserDAO.find { UserTable.email eq forgetPasswordRequest.email }.toList().singleOrNull()
        userEntity?.let {
            val otp = generateOTP()
            it.otpCode = generateOTP()
            otp
        } ?: throw forgetPasswordRequest.email.notFoundException()
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
        val userEntity = UserDAO.find { UserTable.email eq resetPasswordRequest.email }.toList().singleOrNull()
        userEntity?.let {
            if (resetPasswordRequest.verificationCode == it.otpCode) {
                it.password = BCrypt.withDefaults().hashToString(12, resetPasswordRequest.newPassword.toCharArray())
                it.otpCode = it.otpCode
                AppConstants.DataBaseTransaction.FOUND
            } else {
                AppConstants.DataBaseTransaction.NOT_FOUND
            }
        } ?: throw resetPasswordRequest.email.notFoundException()
    }
}
