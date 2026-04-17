package com.piashcse.feature.profile

import com.piashcse.database.entities.UserProfileDAO
import com.piashcse.database.entities.UserProfileTable
import com.piashcse.model.request.UserProfileRequest
import com.piashcse.model.response.UserProfile
import com.piashcse.service.UploadService
import com.piashcse.utils.extension.query
import com.piashcse.utils.throwNotFound
import org.jetbrains.exposed.v1.core.eq

/**
 * Controller for managing user profiles. Provides methods to retrieve, update, and change user profile details and images.
 */
class ProfileService : ProfileRepository {

    // UploadService init block handles directory creation

    /**
     * Retrieves the user profile based on the given user ID.
     *
     * @param userId The ID of the user whose profile is to be retrieved.
     * @return The user profile corresponding to the given user ID.
     * @throws userId.throwNotFound("Resource") If no user profile is found for the given user ID.
     */
    override suspend fun getProfile(userId: String): UserProfile = query {
        val isProfileExist =
            UserProfileDAO.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
        isProfileExist?.response() ?: userId.throwNotFound("User")
    }

    /**
     * Updates the user profile details.
     *
     * @param userId The ID of the user whose profile is to be updated.
     * @param userProfile The new profile details to update, can be null to keep current values.
     * @return The updated user profile.
     * @throws userId.throwNotFound("Resource") If no user profile is found for the given user ID.
     */
    override suspend fun updateProfile(userId: String, userProfile: UserProfileRequest?): UserProfile = query {
        val userProfileEntity =
            UserProfileDAO.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
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
        } ?: userId.throwNotFound("User")
    }

    /**
     * Updates the user's profile image and replaces the old one if it exists.
     *
     * @param userId The ID of the user whose profile image is to be updated.
     * @param imageUrl The new profile image URL.
     * @return The updated user profile with the new image.
     * @throws userId.throwNotFound("Resource") If no user profile is found for the given user ID.
     */
    override suspend fun updateProfileImage(userId: String, imageUrl: String?): UserProfile = query {
        val userProfileEntity =
            UserProfileDAO.find { UserProfileTable.userId eq userId }.toList().singleOrNull()

        // Delete previous profile image if it exists
        userProfileEntity?.image?.let { oldImageUrl ->
            // Extract filename from URL and delete old image
            val oldFileName = oldImageUrl.substringAfterLast("/")
            UploadService.deleteProfileImage(oldFileName)
        }

        userProfileEntity?.let {
            it.image = imageUrl ?: it.image
            it.response()
        } ?: userId.throwNotFound("User")
    }
}