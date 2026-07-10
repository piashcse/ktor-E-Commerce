package com.piashcse.feature.auth

import com.piashcse.database.entities.ChangePassword
import com.piashcse.model.request.ForgotPasswordRequest
import com.piashcse.model.request.ResetRequest
import com.piashcse.model.response.ResetResult
import com.piashcse.utils.email.EmailSender

class PasswordManagementService(private val authRepo: AuthRepository) {

    suspend fun changePassword(userId: String, changePassword: ChangePassword): Boolean =
        authRepo.changePassword(userId, changePassword)

    suspend fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest) {
        val user = authRepo.findResetUserByEmail(forgotPasswordRequest.email, forgotPasswordRequest.userType)
        authRepo.forgotPassword(forgotPasswordRequest)
        EmailSender.sendOtp(user.email, "", "Password Reset")
    }

    suspend fun resetPassword(resetPasswordRequest: ResetRequest): ResetResult =
        authRepo.resetPassword(resetPasswordRequest)
}
