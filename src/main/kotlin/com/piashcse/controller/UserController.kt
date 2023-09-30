package com.piashcse.controller

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.dbhelper.query
import com.piashcse.entities.user.*
import com.piashcse.models.user.body.*
import com.piashcse.models.user.response.RegistrationResponse
import com.piashcse.utils.*
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

class UserController {
    suspend fun registration(registrationBody: RegistrationBody) = query {
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
        RegistrationResponse(inserted.id.value, registrationBody.email)
    }

   suspend fun login(loginBody: LoginBody) = query {
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
        } ?: loginBody.email.isNotExistException()
    }

    suspend fun changePassword(userId: String, changePassword: ChangePassword) = query {
        val userEntity = UsersEntity.find { UserTable.id eq userId }.toList().singleOrNull()
        userEntity?.let {
            if (BCrypt.verifyer().verify(changePassword.oldPassword.toCharArray(), it.password).verified) {
                it.password = BCrypt.withDefaults().hashToString(12, changePassword.newPassword.toCharArray())
                it
            } else {
                changePassword
            }
        }
    }

    suspend fun forgetPassword(forgetPasswordBody: ForgetPasswordEmail) = query {
        val userEntity = UsersEntity.find { UserTable.email eq forgetPasswordBody.email }.toList().singleOrNull()
        userEntity?.let {
            val verificationCode = Random.nextInt(1000, 9999).toString()
            it.verificationCode = verificationCode
            VerificationCode(verificationCode)
        }
    }

   suspend fun changeForgetPasswordByVerificationCode(confirmPasswordBody: ConfirmPassword) = query {
        val userEntity = UsersEntity.find { UserTable.email eq confirmPasswordBody.email }.toList().singleOrNull()
        userEntity?.let {
            if (confirmPasswordBody.verificationCode == it.verificationCode) {
                it.password = BCrypt.withDefaults().hashToString(12, confirmPasswordBody.newPassword.toCharArray())
                it.verificationCode = null
                AppConstants.DataBaseTransaction.FOUND
            } else {
                AppConstants.DataBaseTransaction.NOT_FOUND
            }
        } ?: run {
            confirmPasswordBody.email.isNotExistException()
        }
    }

}
