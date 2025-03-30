package com.piashcse.controller

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.entities.*
import com.piashcse.models.user.body.ConfirmPasswordRequest
import com.piashcse.models.user.body.ForgetPasswordRequest
import com.piashcse.models.user.body.LoginRequest
import com.piashcse.models.user.body.RegisterRequest
import com.piashcse.models.user.response.RegisterResponse
import com.piashcse.repository.AuthRepo
import com.piashcse.utils.AppConstants
import com.piashcse.utils.PasswordNotMatch
import com.piashcse.utils.UserNotExistException
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.sql.and
import kotlin.random.Random

class AuthController : AuthRepo {
    /**
     * Registers a new user with the given [registerRequest].
     * Throws an exception if the user already exists.
     *
     * @param registerRequest The request containing user details.
     * @return The response containing the registered user ID and email.
     */
    override suspend fun register(registerRequest: RegisterRequest): RegisterResponse = query {
        val userEntity =
            UsersEntity.find { UserTable.email eq registerRequest.email and (UserTable.userType eq registerRequest.userType) }
                .toList().singleOrNull()
        userEntity?.let {
            it.id.value.alreadyExistException("as ${it.userType}")
        }
        val inserted = UsersEntity.new {
            email = registerRequest.email
            password = BCrypt.withDefaults().hashToString(12, registerRequest.password.toCharArray())
            userType = registerRequest.userType
        }
        UsersProfileEntity.new {
            userId = inserted.id
        }
        RegisterResponse(inserted.id.value, registerRequest.email)
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
            UsersEntity.find { UserTable.email eq loginRequest.email and (UserTable.userType eq loginRequest.userType) }
                .toList().singleOrNull()
        userEntity?.let {
            if (BCrypt.verifyer().verify(
                    loginRequest.password.toCharArray(), it.password
                ).verified
            ) {
                it.loggedInWithToken()
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
        val userEntity = UsersEntity.find { UserTable.id eq userId }.toList().singleOrNull()
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
    override suspend fun sendPasswordResetOtp(forgetPasswordRequest: ForgetPasswordRequest): VerificationCode = query {
        val userEntity = UsersEntity.find { UserTable.email eq forgetPasswordRequest.email }.toList().singleOrNull()
        userEntity?.let {
            val verificationCode = Random.nextInt(1000, 9999).toString()
            it.verificationCode = verificationCode
            VerificationCode(verificationCode)
        } ?: throw forgetPasswordRequest.email.notFoundException()
    }

    /**
     * Verifies the password reset code and updates the password if the code is valid.
     * If the verification code matches, the password is updated and the code is cleared.
     * Returns a constant indicating whether the operation was successful or not.
     *
     * @param confirmPasswordRequest The request containing email, verification code, and new password.
     * @return `FOUND` if the verification code is correct and the password is updated, otherwise `NOT_FOUND`.
     * @throws Exception if the user does not exist.
     */
    override suspend fun verifyPasswordResetOtp(confirmPasswordRequest: ConfirmPasswordRequest): Int = query {
        val userEntity = UsersEntity.find { UserTable.email eq confirmPasswordRequest.email }.toList().singleOrNull()
        userEntity?.let {
            if (confirmPasswordRequest.verificationCode == it.verificationCode) {
                it.password = BCrypt.withDefaults().hashToString(12, confirmPasswordRequest.newPassword.toCharArray())
                it.verificationCode = null
                AppConstants.DataBaseTransaction.FOUND
            } else {
                AppConstants.DataBaseTransaction.NOT_FOUND
            }
        } ?: throw confirmPasswordRequest.email.notFoundException()
    }
}
