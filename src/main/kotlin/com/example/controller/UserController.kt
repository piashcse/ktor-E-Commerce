package com.example.controller

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.entities.user.*
import com.example.models.user.body.*
import com.example.models.user.response.RegistrationResponse
import com.example.utils.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.random.Random

class UserController {
    fun registration(registrationBody: RegistrationBody) = transaction {
        val userEntity = UsersEntity.find { UserTable.email eq registrationBody.email }.toList().singleOrNull()
        return@transaction if (userEntity == null) {
            val inserted = UsersEntity.new {
                email = registrationBody.email
                password = BCrypt.withDefaults().hashToString(12, registrationBody.password.toCharArray())
            }
            UsersProfileEntity.new {
                userId = inserted.id
            }
            UserHasTypeEntity.new {
                userId = inserted.id
                userTypeId = registrationBody.userType
            }
            RegistrationResponse(registrationBody.email)
        } else {
            throw CommonException("${registrationBody.email} already Exist")
        }
    }

    fun login(loginBody: LoginBody) = transaction {
        val query = UserTable.leftJoin(UserHasTypeTable).select { UserTable.email eq loginBody.email }
        val result = UsersEntity.wrapRows(query).first()
        if (result.userType.userTypeId != loginBody.userType) throw UserTypeException()
        if (BCrypt.verifyer().verify(
                loginBody.password.toCharArray(), result.password
            ).verified && result.userType.userTypeId == loginBody.userType
        ) {
            return@transaction result.loggedInWithToken()
        } else {
            throw PasswordNotMatch()
        }
    }

    fun jwtVerification(jwtTokenBody: JwtTokenBody) = transaction {
        val usersEntity = UsersEntity.find { UserTable.email eq jwtTokenBody.email }.toList().singleOrNull()
        usersEntity?.let {
            return@transaction if (usersEntity != null) {
                JwtTokenBody(usersEntity.id.value, usersEntity.email, usersEntity.userType.userTypeId)
            } else null
        }
    }

    fun changePassword(userId: String, changePassword: ChangePassword) = transaction {
        val userEntity = UsersEntity.find { UserTable.id eq userId }.toList().singleOrNull()
        return@transaction userEntity?.let {
            if (BCrypt.verifyer().verify(changePassword.oldPassword.toCharArray(), it.password).verified) {
                it.password = BCrypt.withDefaults().hashToString(12, changePassword.newPassword.toCharArray())
                it
            } else {
                return@transaction changePassword
            }
        }
    }

    fun forgetPassword(forgetPasswordBody: ForgetPasswordBody) = transaction {
        val userEntity = UsersEntity.find { UserTable.email eq forgetPasswordBody.email }.toList().singleOrNull()
        return@transaction userEntity?.let {
            val verificationCode = Random.nextInt(1000, 9999).toString()
            it.verificationCode = verificationCode
            VerificationCode(verificationCode)
        } ?: run {
            throw CommonException("${forgetPasswordBody.email} is not exist")
        }
    }

    fun confirmPassword(confirmPasswordBody: ConfirmPasswordBody) = transaction {
        val userEntity = UsersEntity.find { UserTable.email eq confirmPasswordBody.email }.toList().singleOrNull()
        return@transaction userEntity?.let {
            if (confirmPasswordBody.verificationCode == it.verificationCode) {
                it.password = BCrypt.withDefaults().hashToString(12, confirmPasswordBody.password.toCharArray())
                it.verificationCode = null
                AppConstants.DataBaseTransaction.FOUND
            } else {
                AppConstants.DataBaseTransaction.NOT_FOUND
            }
        } ?: run {
            throw CommonException("${confirmPasswordBody.email} is not exist")
        }
    }


    fun updateProfile(userId: String, userProfile: UserProfile?) = transaction {
        val userProfileEntity = UsersProfileEntity.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
        return@transaction userProfileEntity?.let {
            it.userProfileImage = userProfile?.userProfileImage ?: it.userProfileImage
            it.firstName = userProfile?.firstName ?: it.firstName
            it.lastName = userProfile?.lastName ?: it.lastName
            it.secondaryMobileNumber = userProfile?.secondaryMobileNumber ?: it.secondaryMobileNumber
            it.faxNumber = userProfile?.faxNumber ?: it.faxNumber
            it.streetAddress = userProfile?.streetAddress ?: it.streetAddress
            it.city = userProfile?.city ?: it.city
            it.identificationType = userProfile?.identificationType ?: it.identificationType
            it.identificationNo = userProfile?.identificationNo ?: it.identificationNo
            it.occupation = userProfile?.occupation ?: it.occupation
            it.userDescription = userProfile?.userDescription ?: it.userDescription
            it.maritalStatus = userProfile?.maritalStatus ?: it.maritalStatus
            it.postCode = userProfile?.postCode ?: it.postCode
            it.gender = userProfile?.gender ?: it.gender
            it.response()
        }
    }

    fun updateProfileImage(userId: String, profileImage: String?) = transaction {
        val userProfileEntity = UsersProfileEntity.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
        // delete previous file from directory as latest one replace previous one
        userProfileEntity?.userProfileImage?.let {
            Files.deleteIfExists(Paths.get("${AppConstants.Image.PROFILE_IMAGE_LOCATION}$it"))
        }
        return@transaction userProfileEntity?.let {
            it.userProfileImage = profileImage ?: it.userProfileImage
            it.response()
        }
    }

}
