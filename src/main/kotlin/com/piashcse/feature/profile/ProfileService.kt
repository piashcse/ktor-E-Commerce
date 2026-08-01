package com.piashcse.feature.profile

import com.piashcse.model.request.UserProfileRequest
import com.piashcse.model.response.UserProfileResponse
import com.piashcse.service.UploadService

class ProfileService(private val profileRepo: ProfileRepository) {
    suspend fun getProfile(userId: String): UserProfileResponse = profileRepo.getProfile(userId)

    suspend fun updateProfile(
        userId: String,
        profileRequest: UserProfileRequest?,
    ): UserProfileResponse = profileRepo.updateProfile(userId, profileRequest)

    suspend fun updateProfileImage(
        userId: String,
        imageUrl: String?,
    ): UserProfileResponse {
        val oldProfile = profileRepo.getProfile(userId)
        oldProfile.image?.let { oldImageUrl ->
            val oldFileName = oldImageUrl.substringAfterLast("/")
            UploadService.deleteProfileImage(oldFileName)
        }
        return profileRepo.updateProfileImage(userId, imageUrl)
    }
}
