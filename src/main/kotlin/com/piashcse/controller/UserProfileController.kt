package com.piashcse.controller

import com.piashcse.entities.UserProfile
import com.piashcse.entities.UserProfileTable
import com.piashcse.entities.UsersProfileEntity
import com.piashcse.models.user.body.UserProfileBody
import com.piashcse.repository.UserProfileRepo
import com.piashcse.utils.AppConstants
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class UserProfileController : UserProfileRepo {
    init {
        if (!File(AppConstants.ImageFolder.PROFILE_IMAGE_LOCATION).exists()) {
            File(AppConstants.ImageFolder.PROFILE_IMAGE_LOCATION).mkdirs()
        }
    }

    override suspend fun getProfile(userId: String): UserProfile = query {
        val isProfileExist = UsersProfileEntity.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
        isProfileExist?.response() ?: throw userId.notFoundException()
    }

    override suspend fun updateProfileInfo(userId: String, userProfile: UserProfileBody?): UserProfile = query {
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
            it.postCode = userProfile?.postCode ?: it.postCode
            it.gender = userProfile?.gender ?: it.gender
            it.response()
        } ?: throw userId.notFoundException()
    }

    override suspend fun updateProfileImage(userId: String, profileImage: String?): UserProfile = query {
        val userProfileEntity = UsersProfileEntity.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
        // delete previous file from directory as latest one replace previous one
        userProfileEntity?.userProfileImage?.let {
            Files.deleteIfExists(Paths.get("${AppConstants.ImageFolder.PROFILE_IMAGE_LOCATION}$it"))
        }
        userProfileEntity?.let {
            it.userProfileImage = profileImage ?: it.userProfileImage
            it.response()
        } ?: throw userId.notFoundException()
    }
}