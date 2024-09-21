package com.piashcse.controller

import com.piashcse.entities.user.UserProfile
import com.piashcse.entities.user.UserProfileTable
import com.piashcse.entities.user.UsersProfileEntity
import com.piashcse.models.user.body.UserProfileBody
import com.piashcse.repository.UserProfileRepo
import com.piashcse.utils.AppConstants
import com.piashcse.utils.extension.query
import io.ktor.server.plugins.*
import java.nio.file.Files
import java.nio.file.Paths

class UserProfileController : UserProfileRepo {
    override suspend fun getProfile(userId: String): UserProfile = query {
        val isProfileExist = UsersProfileEntity.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
        isProfileExist?.response() ?: throw NotFoundException("User not found")
    }

    override suspend fun updateProfile(userId: String, userProfile: UserProfileBody?): UserProfile = query {
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
        } ?: throw NotFoundException("User not found")
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
        } ?: throw NotFoundException("User not found")
    }
}