package com.piashcse.feature.profile

import com.piashcse.model.request.UserProfileRequest
import com.piashcse.model.response.UserProfile

interface ProfileRepository {
    /**
     * Retrieves the user profile.
     *
     * @param userId The unique identifier of the user.
     * @return The user's profile.
     */
    suspend fun getProfile(userId: String): UserProfile

    /**
     * Updates the user profile information.
     *
     * @param userId The unique identifier of the user.
     * @param profileRequest The updated profile data. Can be null if no update is provided.
     * @return The updated user profile.
     */
    suspend fun updateProfile(userId: String, profileRequest: UserProfileRequest?): UserProfile

    /**
     * Updates the user's profile image.
     *
     * @param userId The unique identifier of the user.
     * @param imageUrl The new profile image URL. Can be null if no image is provided.
     * @return The updated user profile.
     */
    suspend fun updateProfileImage(userId: String, imageUrl: String?): UserProfile
}