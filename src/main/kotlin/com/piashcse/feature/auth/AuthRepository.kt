package com.piashcse.feature.auth

import com.piashcse.constants.UserType
import com.piashcse.database.entities.ChangePassword
import com.piashcse.database.entities.LoginResponse
import com.piashcse.model.request.ForgetPasswordRequest
import com.piashcse.model.request.LoginRequest
import com.piashcse.model.request.RegisterRequest
import com.piashcse.model.request.ResetRequest

interface AuthRepository {
    /**
     * Registers a new user.
     *
     * @param request The registration details.
     * @return The registration response.
     */
    suspend fun register(registerRequest: RegisterRequest): Any

    /**
     * Authenticates a user and returns a login response.
     *
     * @param request The login credentials.
     * @return The login response.
     */
    suspend fun login(loginRequest: LoginRequest): LoginResponse

    /**
     * Otp verification.
     *
     * @param userId The user ID.
     * @param otp The OTP code.
     * @return The success response.
     */
    suspend fun otpVerification(userId: String, otp: String): Boolean

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
    suspend fun forgetPassword(forgetPasswordRequest: ForgetPasswordRequest): String

    /**
     * Verifies the password reset code.
     *
     * @param request The request containing the verification details.
     * @return A status code representing the verification result.
     */
    suspend fun resetPassword(resetPasswordRequest: ResetRequest): Int

    /**
     * Changes the user type for a user.
     *
     * @param currentUserId The ID of the admin making the change.
     * @param targetUserId The ID of the user whose type is being changed.
     * @param newUserType The new user type to assign.
     * @return `true` if the user type was changed successfully, `false` otherwise.
     */
    suspend fun changeUserType(currentUserId: String, targetUserId: String, newUserType: UserType): Boolean

    /**
     * Deactivates a user account.
     *
     * @param currentUserId The ID of the admin making the change.
     * @param targetUserId The ID of the user to deactivate.
     * @return `true` if the user was deactivated successfully, `false` otherwise.
     */
    suspend fun deactivateUser(currentUserId: String, targetUserId: String): Boolean

    /**
     * Activates a user account.
     *
     * @param currentUserId The ID of the admin making the change.
     * @param targetUserId The ID of the user to activate.
     * @return `true` if the user was activated successfully, `false` otherwise.
     */
    suspend fun activateUser(currentUserId: String, targetUserId: String): Boolean
}