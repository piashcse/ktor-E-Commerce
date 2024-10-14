package com.piashcse.controller

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.entities.*
import com.piashcse.models.user.body.ConfirmPassword
import com.piashcse.models.user.body.ForgetPasswordEmail
import com.piashcse.models.user.body.LoginBody
import com.piashcse.models.user.body.RegistrationBody
import com.piashcse.models.user.response.RegistrationSuccessResponse
import com.piashcse.repository.UserRepo
import com.piashcse.utils.AppConstants
import com.piashcse.utils.PasswordNotMatch
import com.piashcse.utils.UserNotExistException
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.sql.and
import kotlin.random.Random

class UserController : UserRepo {
    override suspend fun addUser(registrationBody: RegistrationBody): RegistrationSuccessResponse = query {
        val userEntity =
            UsersEntity.find { UserTable.email eq registrationBody.email and (UserTable.userType eq registrationBody.userType) }
                .toList().singleOrNull()
        userEntity?.let {
            it.id.value.alreadyExistException("as ${it.userType}")
        }
        val inserted = UsersEntity.new {
            email = registrationBody.email
            password = BCrypt.withDefaults().hashToString(12, registrationBody.password.toCharArray())
            userType = registrationBody.userType
        }
        UsersProfileEntity.new {
            userId = inserted.id
        }
        RegistrationSuccessResponse(inserted.id.value, registrationBody.email)
    }

    override suspend fun login(loginBody: LoginBody): LoginResponse = query {
        val userEntity =
            UsersEntity.find { UserTable.email eq loginBody.email and (UserTable.userType eq loginBody.userType) }
                .toList().singleOrNull()
        userEntity?.let {
            if (BCrypt.verifyer().verify(
                    loginBody.password.toCharArray(), it.password
                ).verified
            ) {
                it.loggedInWithToken()
            } else {
                throw PasswordNotMatch()
            }
        } ?: throw loginBody.email.notFoundException()
    }

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

    override suspend fun forgetPasswordSendCode(forgetPasswordBody: ForgetPasswordEmail): VerificationCode = query {
        val userEntity = UsersEntity.find { UserTable.email eq forgetPasswordBody.email }.toList().singleOrNull()
        userEntity?.let {
            val verificationCode = Random.nextInt(1000, 9999).toString()
            it.verificationCode = verificationCode
            VerificationCode(verificationCode)
        } ?: throw forgetPasswordBody.email.notFoundException()
    }

    override suspend fun forgetPasswordVerificationCode(confirmPasswordBody: ConfirmPassword): Int = query {
        val userEntity = UsersEntity.find { UserTable.email eq confirmPasswordBody.email }.toList().singleOrNull()
        userEntity?.let {
            if (confirmPasswordBody.verificationCode == it.verificationCode) {
                it.password = BCrypt.withDefaults().hashToString(12, confirmPasswordBody.newPassword.toCharArray())
                it.verificationCode = null
                AppConstants.DataBaseTransaction.FOUND
            } else {
                AppConstants.DataBaseTransaction.NOT_FOUND
            }
        } ?: throw confirmPasswordBody.email.notFoundException()
    }

}
