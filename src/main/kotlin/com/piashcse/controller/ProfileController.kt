package com.piashcse.controller

import com.piashcse.dbhelper.query
import com.piashcse.entities.user.UserProfileTable
import com.piashcse.entities.user.UsersProfileEntity
import com.piashcse.models.user.body.UserProfileBody
import com.piashcse.utils.AppConstants
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import java.nio.file.Paths

class ProfileController {
    suspend fun getProfile(userId: String) = query {
        val profile = UsersProfileEntity.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
        profile?.let {
            it.response()
        }
    }

    suspend fun updateProfile(userId: String, userProfile: UserProfileBody?) = query {
        val userProfileEntity = UsersProfileEntity.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
        userProfileEntity?.let {
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

    suspend fun updateProfileImage(userId: String, profileImage: String?) = query {
        val userProfileEntity = UsersProfileEntity.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
        // delete previous file from directory as latest one replace previous one
        userProfileEntity?.userProfileImage?.let {
            Files.deleteIfExists(Paths.get("${AppConstants.Image.PROFILE_IMAGE_LOCATION}$it"))
        }
        userProfileEntity?.let {
            it.userProfileImage = profileImage ?: it.userProfileImage
            it.response()
        }
    }
}