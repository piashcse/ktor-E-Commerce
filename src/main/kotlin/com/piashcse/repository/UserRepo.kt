package com.piashcse.repository

import com.piashcse.entities.user.ChangePassword
import com.piashcse.entities.user.LoginResponse
import com.piashcse.entities.user.VerificationCode
import com.piashcse.models.user.body.ConfirmPassword
import com.piashcse.models.user.body.ForgetPasswordEmail
import com.piashcse.models.user.body.LoginBody
import com.piashcse.models.user.body.RegistrationBody
import com.piashcse.models.user.response.RegistrationSuccessResponse

interface UserRepo {
    suspend fun addUser(registrationBody: RegistrationBody): RegistrationSuccessResponse
    suspend fun login(loginBody: LoginBody): LoginResponse
    suspend fun changePassword(userId: String, changePassword: ChangePassword): Boolean
    suspend fun forgetPasswordSendCode(forgetPasswordBody: ForgetPasswordEmail): VerificationCode
    suspend fun forgetPasswordVerificationCode(confirmPasswordBody: ConfirmPassword): Int
}