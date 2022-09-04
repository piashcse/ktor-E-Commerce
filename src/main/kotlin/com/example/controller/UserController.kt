package com.example.controller

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.entities.user.*
import com.example.models.user.*
import com.example.utils.*
import com.example.utils.extension.currentTimeInUTC
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

class UserController {
    fun registration(registrationBody: RegistrationBody) = transaction {
        val userEntity = UsersEntity.find { UserTable.email eq registrationBody.email }.toList().singleOrNull()
        return@transaction if (userEntity == null) {
            val inserted = UsersEntity.new {
                user_name = registrationBody.userName
                email = registrationBody.email
                password = BCrypt.withDefaults().hashToString(12, registrationBody.password.toCharArray())
            }
            UsersProfileEntity.new {
                user_id = inserted.id
            }
            UserHasTypeEntity.new{
                user_id = inserted.id
                user_type_id = registrationBody.userType
            }
            RegistrationResponse(registrationBody.userName, registrationBody.email)
        } else {
            throw CommonException("${registrationBody.email} already Exist")
        }
    }

    fun login(loginBody: LoginBody) = transaction {
        val query = UserTable.leftJoin(UserHasTypeTable).select { UserTable.email eq loginBody.email }
        val result = UsersEntity.wrapRows(query).first()
        if (result.userType.user_type_id != loginBody.userType) throw UserTypeException()
        if (BCrypt.verifyer().verify(
                loginBody.password.toCharArray(), result.password
            ).verified && result.userType.user_type_id == loginBody.userType
        ) {
            return@transaction result.response()
        } else {
            throw PasswordNotMatch()
        }
    }

    fun jwtVerification(jwtTokenBody: JwtTokenBody) = transaction {
        val usersEntity = UsersEntity.find { UserTable.email eq jwtTokenBody.email }.toList().singleOrNull()
        usersEntity?.let {
            return@transaction if (usersEntity != null) {
                JwtTokenBody(usersEntity.id.value, usersEntity.email, usersEntity.userType.user_type_id)
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
            it.verification_code = verificationCode
            VerificationCode(verificationCode)
        } ?: run {
            throw CommonException("${forgetPasswordBody.email} is not exist")
        }
    }

    fun confirmPassword(confirmPasswordBody: ConfirmPasswordBody) = transaction {
        val userEntity = UsersEntity.find { UserTable.email eq confirmPasswordBody.email }.toList().singleOrNull()
        return@transaction userEntity?.let {
            if (confirmPasswordBody.verificationCode == it.verification_code) {
                it.password = BCrypt.withDefaults().hashToString(12, confirmPasswordBody.password.toCharArray())
                it.verification_code = null
                AppConstants.DataBaseTransaction.FOUND
            } else {
                AppConstants.DataBaseTransaction.NOT_FOUND
            }
        } ?: run {
            throw CommonException("${confirmPasswordBody.email} is not exist")
        }
    }


    fun updateProfile(userId: String, userProfile: UserProfile?) = transaction {
        val userProfileEntity = UsersProfileEntity.find { UserProfileTable.user_id eq userId }.toList().singleOrNull()
        return@transaction userProfileEntity?.let {
            it.user_profile_image = userProfile?.userProfileImage ?: it.user_profile_image
            it.first_name = userProfile?.firstName ?: it.first_name
            it.last_name = userProfile?.lastName ?: it.last_name
            it.secondary_mobile_number = userProfile?.secondaryMobileNumber ?: it.secondary_mobile_number
            it.fax_number = userProfile?.faxNumber ?: it.fax_number
            it.street_address = userProfile?.streetAddress ?: it.street_address
            it.city = userProfile?.city ?: it.city
            it.identification_type = userProfile?.identificationType ?: it.identification_type
            it.identification_no = userProfile?.identificationNo ?: it.identification_no
            it.occupation = userProfile?.occupation ?: it.occupation
            it.user_description = userProfile?.userDescription ?: it.user_description
            it.marital_status = userProfile?.maritalStatus ?: it.marital_status
            it.post_code = userProfile?.postCode ?: it.post_code
            it.gender = userProfile?.gender ?: it.gender
            it.response()
        }
    }

    fun updateProfileImage(userId: String, profileImage: String?) = transaction {
        val userProfileEntity = UsersProfileEntity.find { UserProfileTable.user_id eq userId }.toList().singleOrNull()
        return@transaction userProfileEntity?.let {
            it.user_profile_image = profileImage ?: it.user_profile_image
            it.response()
        }
    }

}
