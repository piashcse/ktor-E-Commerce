package com.piashcse.repository

import com.piashcse.entities.user.UserProfile
import com.piashcse.models.user.body.UserProfileBody

interface UserProfileRepo {
    suspend fun getProfile(userId: String): UserProfile
    suspend fun updateProfileInfo(userId: String, userProfile: UserProfileBody?): UserProfile
    suspend fun updateProfileImage(userId: String, profileImage: String?): UserProfile
}