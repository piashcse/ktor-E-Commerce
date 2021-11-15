package com.example.entities

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object UsersProfileTable : IdTable<String>("users_profile") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()

    //val user_id = text("user_id").references(UsersTable.id)
    val user_id = reference("user_id", UsersTable.id)
    val user_profile_image = text("user_profile_image").nullable()
    val first_name = text("first_name").nullable()
    val last_name = text("last_name").nullable()
    val secondary_mobile_number = text("secondary_mobile_number").nullable()
    val fax_number = text("fax_number").nullable()
    val street_address = text("street_address").nullable()
    val city = text("city").nullable()
    val identification_type = text("identification_type").nullable()
    val identification_no = text("identification_no").nullable()
    val occupation = text("occupation").nullable()
    val user_description = text("user_description").nullable()
    val marital_status = text("marital_status").nullable()
    val post_code = text("post_code").nullable()
    val gender = text("gender").nullable()
    val created_at = datetime("created_at").defaultExpression(CurrentDateTime()) // UTC time
    val updated_at = datetime("updated_at")
    override val primaryKey = PrimaryKey(id)
}

class UsersProfileEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UsersProfileEntity>(UsersProfileTable)
    var profileId by UsersProfileTable.id
    var user_id by UsersProfileTable.user_id
    var user_profile_image by UsersProfileTable.user_profile_image
    var first_name by UsersProfileTable.first_name
    var last_name by UsersProfileTable.last_name
    var secondary_mobile_number by UsersProfileTable.secondary_mobile_number
    var fax_number by UsersProfileTable.fax_number
    var street_address by UsersProfileTable.street_address
    var city by UsersProfileTable.city
    var identification_type by UsersProfileTable.identification_type
    var identification_no by UsersProfileTable.identification_no
    var occupation by UsersProfileTable.occupation
    var user_description by UsersProfileTable.user_description
    var marital_status by UsersProfileTable.marital_status
    var post_code by UsersProfileTable.post_code
    var gender by UsersProfileTable.gender
    var created_at by UsersProfileTable.created_at
    var updated_at by UsersProfileTable.updated_at
    fun userProfileResponse() = UserProfile(
        user_id.value,
        user_profile_image,
        first_name,
        last_name,
        secondary_mobile_number,
        fax_number,
        street_address,
        city,
        identification_type,
        identification_no,
        occupation,
        user_description,
        marital_status,
        post_code,
        gender,
        created_at.toString(),
        updated_at.toString()
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
    val gender: String?,
    val createdAt: String,
    val updatedAt: String
)

