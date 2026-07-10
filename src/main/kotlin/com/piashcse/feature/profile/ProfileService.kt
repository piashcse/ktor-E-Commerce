package com.piashcse.feature.profile

import com.piashcse.model.response.UserProfileResponse
import com.piashcse.service.UploadService

class ProfileService(private val repo: ProfileRepository) : ProfileRepository by repo {
    override suspend fun updateProfileImage(
        userId: String,
        imageUrl: String?,
    ): UserProfileResponse {
        val oldProfile = repo.getProfile(userId)
        oldProfile.image?.let { oldImageUrl ->
            val oldFileName = oldImageUrl.substringAfterLast("/")
            UploadService.deleteProfileImage(oldFileName)
        }
        return repo.updateProfileImage(userId, imageUrl)
    }
}
