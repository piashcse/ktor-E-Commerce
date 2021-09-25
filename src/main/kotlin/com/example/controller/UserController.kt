package com.example.controller

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.entities.*
import com.example.models.JwtTokenBody
import com.example.models.LoginBody
import com.example.models.RegistrationResponse
import com.example.models.RegistrationBody
import com.example.utils.currentTimeInUTC
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class UserController {
    fun registration(registrationBody: RegistrationBody) = transaction {
        val userEntity = UsersEntity.find { UsersTable.email eq registrationBody.email }.toList().singleOrNull()
        return@transaction if (userEntity == null) {
            val inserted = UsersEntity.new {
                userId = EntityID(UUID.randomUUID().toString(), UsersTable)
                user_name = registrationBody.userName
                email = registrationBody.email
                password = BCrypt.withDefaults().hashToString(12, registrationBody.password.toCharArray())
                created_at = currentTimeInUTC().toString()
                updated_at = currentTimeInUTC().toString()
            }
            UsersProfileEntity.new {
                profileId = EntityID(UUID.randomUUID().toString(), UsersProfileTable)
                user_id = inserted.userId.value
                created_at = currentTimeInUTC().toString()
                updated_at = currentTimeInUTC().toString()
            }
            RegistrationResponse(registrationBody.userName, registrationBody.email)
        } else null
    }

    fun login(loginBody: LoginBody) = transaction {
        return@transaction UsersEntity.find { UsersTable.email eq loginBody.email }.toList().singleOrNull()
    }

    fun jwtVerification(jwtTokenBody: JwtTokenBody) = transaction {
        val usersEntity = UsersEntity.find { UsersTable.email eq jwtTokenBody.email }.toList().singleOrNull()
        return@transaction if (usersEntity != null) {
            JwtTokenBody(usersEntity.id.value, usersEntity.email)
        } else null
    }

    fun changePassword(userId: String, changePassword: ChangePassword) = transaction {
        val userEntity = UsersEntity.find { UsersTable.id eq userId }.toList().singleOrNull()
        return@transaction userEntity?.let {
            if (BCrypt.verifyer().verify(changePassword.oldPassword.toCharArray(), it.password).verified) {
                it.password = BCrypt.withDefaults().hashToString(12, changePassword.newPassword.toCharArray())
                it
            } else {
                return@transaction changePassword
            }
        } ?: run {
            null
        }
    }

    fun forgetPassword() {

    }

    fun updateProfile(userId: String, userProfile: UserProfile?) = transaction {
        val userProfileEntity = UsersProfileEntity.find { UsersProfileTable.user_id eq userId }.toList().singleOrNull()
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
            it.updated_at = currentTimeInUTC().toString()
            it
        } ?: run {
            null
        }
    }

}