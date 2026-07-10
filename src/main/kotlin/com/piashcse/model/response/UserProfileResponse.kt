package com.piashcse.model.response

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    var userId: String,
    val image: String?,
    val firstName: String?,
    val lastName: String?,
    val mobile: String?,
    val faxNumber: String?,
    val streetAddress: String?,
    val city: String?,
    val state: String? = null,
    val country: String? = null,
    val identificationType: String?,
    val identificationNo: String?,
    val occupation: String?,
    val postCode: String?,
    val gender: String?,
    val dateOfBirth: @Contextual LocalDate? = null,
    val bio: String? = null,
    val isActive: Boolean = true,
    val verified: Boolean = false,
)
