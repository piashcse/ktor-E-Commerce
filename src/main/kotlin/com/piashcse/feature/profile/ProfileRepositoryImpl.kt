package com.piashcse.feature.profile

import com.piashcse.database.entities.UserProfileDAO
import com.piashcse.database.entities.UserProfileTable
import com.piashcse.model.request.UserProfileRequest
import com.piashcse.model.response.UserProfileResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import org.jetbrains.exposed.v1.core.eq

class ProfileRepositoryImpl : ProfileRepository {
    override suspend fun getProfile(userId: String): UserProfileResponse = query {
        val isProfileExist =
            UserProfileDAO.find { UserProfileTable.userId eq userId }.firstOrNull()
        isProfileExist?.response() ?: userId.throwNotFound("User")
    }

    override suspend fun updateProfile(
        userId: String,
        userProfile: UserProfileRequest?,
    ): UserProfileResponse = query {
        val userProfileEntity =
            UserProfileDAO.find { UserProfileTable.userId eq userId }.firstOrNull()
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

    override suspend fun updateProfileImage(
        userId: String,
        imageUrl: String?,
    ): UserProfileResponse = query {
        val userProfileEntity =
            UserProfileDAO.find { UserProfileTable.userId eq userId }.firstOrNull()
        userProfileEntity?.let {
            it.image = imageUrl ?: it.image
            it.response()
        } ?: userId.throwNotFound("User")
    }
}
