package com.example.entities.user

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object UserProfileTable : BaseIntIdTable("users_profile") {
    val userId = reference("user_id", UserTable.id)
    val userProfileImage = text("user_profile_image").nullable()
    val firstName = text("first_name").nullable()
    val lastName = text("last_name").nullable()
    val secondaryMobileNumber = text("secondary_mobile_number").nullable()
    val faxNumber = text("fax_number").nullable()
    val streetAddress = text("street_address").nullable()
    val city = text("city").nullable()
    val identificationType = text("identification_type").nullable()
    val identificationNo = text("identification_no").nullable()
    val occupation = text("occupation").nullable()
    val userDescription = text("user_description").nullable()
    val maritalStatus = text("marital_status").nullable()
    val postCode = text("post_code").nullable()
    val gender = text("gender").nullable()
}

class UsersProfileEntity(id: EntityID<String>) : BaseIntEntity(id, UserProfileTable) {
    companion object : BaseIntEntityClass<UsersProfileEntity>(UserProfileTable)

    var userId by UserProfileTable.userId
    var userProfileImage by UserProfileTable.userProfileImage
    var firstName by UserProfileTable.firstName
    var lastName by UserProfileTable.lastName
    var secondaryMobileNumber by UserProfileTable.secondaryMobileNumber
    var faxNumber by UserProfileTable.faxNumber
    var streetAddress by UserProfileTable.streetAddress
    var city by UserProfileTable.city
    var identificationType by UserProfileTable.identificationType
    var identificationNo by UserProfileTable.identificationNo
    var occupation by UserProfileTable.occupation
    var userDescription by UserProfileTable.userDescription
    var maritalStatus by UserProfileTable.maritalStatus
    var postCode by UserProfileTable.postCode
    var gender by UserProfileTable.gender
    fun response() = UserProfile(
        userId.value,
        userProfileImage,
        firstName,
        lastName,
        secondaryMobileNumber,
        faxNumber,
        streetAddress,
        city,
        identificationType,
        identificationNo,
        occupation,
        userDescription,
        maritalStatus,
        postCode,
        gender,
    )
}

data class UserProfile(
    var userId: String,
    val userProfileImage: String?,
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

