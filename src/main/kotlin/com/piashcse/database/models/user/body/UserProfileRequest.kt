package com.piashcse.database.models.user.body

data class UserProfileRequest(
    val firstName: String?,
    val lastName: String?,
    val mobile: String?,
    val faxNumber: String?,
    val streetAddress: String?,
    val city: String?,
    val identificationType: String?,
    val identificationNo: String?,
    val occupation: String?,
    val postCode: String?,
    val gender: String?
)