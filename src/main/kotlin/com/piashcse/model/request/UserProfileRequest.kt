package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.validate

@Serializable
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
    val gender: String?,
) {
    init {
        // All fields are optional for partial updates, so each constraint is
        // registered only when a value was supplied.
        firstName?.let { validate(this) { validate(UserProfileRequest::firstName).isNotEmpty() } }
        lastName?.let { validate(this) { validate(UserProfileRequest::lastName).isNotEmpty() } }
        mobile?.let { validate(this) { validate(UserProfileRequest::mobile).isNotEmpty() } }
        faxNumber?.let { validate(this) { validate(UserProfileRequest::faxNumber).isNotEmpty() } }
        streetAddress?.let { validate(this) { validate(UserProfileRequest::streetAddress).isNotEmpty() } }
        city?.let { validate(this) { validate(UserProfileRequest::city).isNotEmpty() } }
        identificationType?.let { validate(this) { validate(UserProfileRequest::identificationType).isNotEmpty() } }
        identificationNo?.let { validate(this) { validate(UserProfileRequest::identificationNo).isNotEmpty() } }
        occupation?.let { validate(this) { validate(UserProfileRequest::occupation).isNotEmpty() } }
        postCode?.let { validate(this) { validate(UserProfileRequest::postCode).isNotEmpty() } }
        gender?.let { validate(this) { validate(UserProfileRequest::gender).isNotEmpty() } }
    }
}
