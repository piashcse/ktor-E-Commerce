package com.piashcse.modules.profile.controller

import com.piashcse.database.entities.UserProfile
import com.piashcse.database.entities.UserProfileTable
import com.piashcse.database.entities.UsersProfileDAO
import com.piashcse.database.models.user.body.UserProfileRequest
import com.piashcse.modules.profile.repository.ProfileRepo
import com.piashcse.utils.AppConstants
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Controller for managing user profiles. Provides methods to retrieve, update, and change user profile details and images.
 */
class ProfileController : ProfileRepo {

    init {
        // Create the profile image directory if it does not exist.
        if (!File(AppConstants.ImageFolder.PROFILE_IMAGE_LOCATION).exists()) {
            File(AppConstants.ImageFolder.PROFILE_IMAGE_LOCATION).mkdirs()
        }
    }

    /**
     * Retrieves the user profile based on the given user ID.
     *
     * @param userId The ID of the user whose profile is to be retrieved.
     * @return The user profile corresponding to the given user ID.
     * @throws userId.notFoundException() If no user profile is found for the given user ID.
     */
    override suspend fun getProfile(userId: String): UserProfile = query {
        val isProfileExist =
            UsersProfileDAO.Companion.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
        isProfileExist?.response() ?: throw userId.notFoundException()
    }

    /**
     * Updates the user profile details.
     *
     * @param userId The ID of the user whose profile is to be updated.
     * @param userProfile The new profile details to update, can be null to keep current values.
     * @return The updated user profile.
     * @throws userId.notFoundException() If no user profile is found for the given user ID.
     */
    override suspend fun updateProfile(userId: String, userProfile: UserProfileRequest?): UserProfile = query {
        val userProfileEntity =
            UsersProfileDAO.Companion.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
        userProfileEntity?.let {
            it.firstName = userProfile?.firstName ?: it.firstName
            it.lastName = userProfile?.lastName ?: it.lastName
            it.mobile = userProfile?.mobile ?: it.mobile
            it.faxNumber = userProfile?.faxNumber ?: it.faxNumber
            it.streetAddress = userProfile?.streetAddress ?: it.streetAddress
            it.city = userProfile?.city ?: it.city
            it.identificationType = userProfile?.identificationType ?: it.identificationType
            it.identificationNo = userProfile?.identificationNo ?: it.identificationNo
            it.occupation = userProfile?.occupation ?: it.occupation
            it.postCode = userProfile?.postCode ?: it.postCode
            it.gender = userProfile?.gender ?: it.gender
            it.response()
        } ?: throw userId.notFoundException()
    }

    /**
     * Updates the user's profile image and replaces the old one if it exists.
     *
     * @param userId The ID of the user whose profile image is to be updated.
     * @param profileImage The new profile image file name.
     * @return The updated user profile with the new image.
     * @throws userId.notFoundException() If no user profile is found for the given user ID.
     */
    override suspend fun updateProfileImage(userId: String, profileImage: String?): UserProfile = query {
        val userProfileEntity =
            UsersProfileDAO.Companion.find { UserProfileTable.userId eq userId }.toList().singleOrNull()

        // Delete previous profile image if it exists, as the new one will replace it.
        userProfileEntity?.image?.let {
            Files.deleteIfExists(Paths.get("${AppConstants.ImageFolder.PROFILE_IMAGE_LOCATION}$it"))
        }

        userProfileEntity?.let {
            it.image = profileImage ?: it.image
            it.response()
        } ?: throw userId.notFoundException()
    }
}