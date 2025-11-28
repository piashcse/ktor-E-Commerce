package com.piashcse.database.entities


import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import com.piashcse.model.response.UserProfile
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.datetime.date

object UserProfileTable : BaseIntIdTable("user_profile") {
    val userId = reference("user_id", UserTable.id)
    val image = text("image").nullable()
    val firstName = text("first_name").nullable()
    val lastName = text("last_name").nullable()
    val mobile = text("mobile").nullable()
    val faxNumber = text("fax_number").nullable()
    val streetAddress = text("street_address").nullable()
    val city = text("city").nullable()
    val state = text("state").nullable() // Added state field
    val country = text("country").nullable() // Added country field
    val identificationType = text("identification_type").nullable()
    val identificationNo = text("identification_no").nullable()
    val occupation = text("occupation").nullable()
    val postCode = text("post_code").nullable()
    val gender = text("gender").nullable()
    val dateOfBirth = date("date_of_birth").nullable() // Added date of birth
    val bio = text("bio").nullable() // Added bio for user description
    val isActive = bool("is_active").default(true) // Whether the profile is active
    val verified = bool("verified").default(false) // Whether the profile is verified
    // createdAt and updatedAt are inherited from BaseIntIdTable
}

class UserProfileDAO(id: EntityID<String>) : BaseIntEntity(id, UserProfileTable) {
    companion object : BaseIntEntityClass<UserProfileDAO>(UserProfileTable, UserProfileDAO::class.java)

    var userId by UserProfileTable.userId
    var image by UserProfileTable.image
    var firstName by UserProfileTable.firstName
    var lastName by UserProfileTable.lastName
    var mobile by UserProfileTable.mobile
    var faxNumber by UserProfileTable.faxNumber
    var streetAddress by UserProfileTable.streetAddress
    var city by UserProfileTable.city
    var state by UserProfileTable.state
    var country by UserProfileTable.country
    var identificationType by UserProfileTable.identificationType
    var identificationNo by UserProfileTable.identificationNo
    var occupation by UserProfileTable.occupation
    var postCode by UserProfileTable.postCode
    var gender by UserProfileTable.gender
    var dateOfBirth by UserProfileTable.dateOfBirth
    var bio by UserProfileTable.bio
    var isActive by UserProfileTable.isActive
    var verified by UserProfileTable.verified

    fun response() = UserProfile(
        userId = userId.value,
        image = image,
        firstName = firstName,
        lastName = lastName,
        mobile = mobile,
        faxNumber = faxNumber,
        streetAddress = streetAddress,
        city = city,
        identificationType = identificationType,
        identificationNo = identificationNo,
        occupation = occupation,
        postCode = postCode,
        gender = gender,
    )
}
