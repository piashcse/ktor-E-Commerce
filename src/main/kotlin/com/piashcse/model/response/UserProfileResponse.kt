package com.piashcse.model.response


data class UserProfile(
    var userId: String,
    val image: String?,
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
