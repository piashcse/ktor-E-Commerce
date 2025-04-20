package com.piashcse.database.entities


import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object UserProfileTable : BaseIntIdTable("user_profile") {
    val userId = reference("user_id", UserTable.id)
    val image = text("image").nullable()
    val firstName = text("first_name").nullable()
    val lastName = text("last_name").nullable()
    val mobile = text("mobile").nullable()
    val faxNumber = text("fax_number").nullable()
    val streetAddress = text("street_address").nullable()
    val city = text("city").nullable()
    val identificationType = text("identification_type").nullable()
    val identificationNo = text("identification_no").nullable()
    val occupation = text("occupation").nullable()
    val postCode = text("post_code").nullable()
    val gender = text("gender").nullable()
}

class UsersProfileDAO(id: EntityID<String>) : BaseIntEntity(id, UserProfileTable) {
    companion object : BaseIntEntityClass<UsersProfileDAO>(UserProfileTable)

    var userId by UserProfileTable.userId
    var image by UserProfileTable.image
    var firstName by UserProfileTable.firstName
    var lastName by UserProfileTable.lastName
    var mobile by UserProfileTable.mobile
    var faxNumber by UserProfileTable.faxNumber
    var streetAddress by UserProfileTable.streetAddress
    var city by UserProfileTable.city
    var identificationType by UserProfileTable.identificationType
    var identificationNo by UserProfileTable.identificationNo
    var occupation by UserProfileTable.occupation
    var postCode by UserProfileTable.postCode
    var gender by UserProfileTable.gender
    fun response() = UserProfile(
        userId.value,
        image,
        firstName,
        lastName,
        mobile,
        faxNumber,
        streetAddress,
        city,
        identificationType,
        identificationNo,
        occupation,
        postCode,
        gender,
    )
}

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

