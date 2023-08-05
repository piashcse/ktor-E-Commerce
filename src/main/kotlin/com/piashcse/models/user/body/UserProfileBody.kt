package com.piashcse.models.user.body

data class UserProfileBody(
    val firstName: String?,
    val lastName: String?,
    val secondaryMobileNumber: String?,
    val faxNumber: String?,
    val streetAddress: String?,
    val city: String?,
    val identificationType: String?,
    val identificationNo: String?,
    val occupation: String?,
    val userDescription: String?,
    val maritalStatus: String?,
    val postCode: String?,
    val gender: String?
)