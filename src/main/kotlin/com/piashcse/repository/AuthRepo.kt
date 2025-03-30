package com.piashcse.repository

import com.piashcse.entities.ChangePassword
import com.piashcse.entities.LoginResponse
import com.piashcse.entities.VerificationCode
import com.piashcse.models.user.body.ConfirmPasswordRequest
import com.piashcse.models.user.body.ForgetPasswordRequest
import com.piashcse.models.user.body.LoginRequest
import com.piashcse.models.user.body.RegisterRequest
import com.piashcse.models.user.response.RegisterResponse

interface AuthRepo {
    /**
     * Registers a new user.
     *
     * @param request The registration details.
     * @return The registration response.
     */
    suspend fun register(registerRequest: RegisterRequest): RegisterResponse

    /**
     * Authenticates a user and returns a login response.
     *
     * @param request The login credentials.
     * @return The login response.
     */
    suspend fun login(loginRequest: LoginRequest): LoginResponse

    /**
     * Changes the password for a user.
     *
     * @param userId The unique identifier of the user.
     * @param request The password change details.
     * @return `true` if the password change was successful, `false` otherwise.
     */
    suspend fun changePassword(userId: String, changePassword: ChangePassword): Boolean

    /**
     * Sends a verification code for password reset.
     *
     * @param request The request containing user details.
     * @return The verification code sent to the user.
     */
    suspend fun sendPasswordResetOtp(forgetPasswordRequest: ForgetPasswordRequest): VerificationCode

    /**
     * Verifies the password reset code.
     *
     * @param request The request containing the verification details.
     * @return A status code representing the verification result.
     */
    suspend fun verifyPasswordResetOtp(confirmPasswordRequest: ConfirmPasswordRequest): Int
}