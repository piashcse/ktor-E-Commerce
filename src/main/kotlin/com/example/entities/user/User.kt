package com.example.entities.user

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object UserTable : IdTable<String>("users") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val user_name = text("user_name")
    val email = text("email")
    val password = text("password")
    val mobile_number = text("mobile_number").nullable()
    val email_verified_at = text("email_verified_at").nullable() // so far unkmown
    val remember_token = text("remember_token").nullable()
    val verification_code = text("verification_code").nullable() // verification_code
    val created_at = datetime("created_at").defaultExpression(CurrentDateTime()) // UTC time
    val updated_at = datetime("updated_at").nullable()
    val is_verified = text("is_verified").nullable() // email verified by validation code
    override val primaryKey = PrimaryKey(id)
}

class UsersEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UsersEntity>(UserTable)
    var user_name by UserTable.user_name
    var email by UserTable.email
    var password by UserTable.password
    var mobile_number by UserTable.mobile_number
    var email_verified_at by UserTable.email_verified_at
    var remember_token by UserTable.remember_token
    var verification_code by UserTable.verification_code
    var created_at by UserTable.created_at
    var updated_at by UserTable.updated_at
    var is_verified by UserTable.is_verified
    val userType by UserHasTypeEntity backReferencedOn UserHasTypeTable.user_id
    fun response() = UsersResponse(
        id.value,
        user_name,
        email,
        mobile_number,
        email_verified_at,
        remember_token,
        is_verified,
        userType.userHasTypeResponse()
    )
}

data class UsersResponse(
    val id: String,
    val userName: String,
    val email: String,
    val mobileNumber: String?,
    val emailVerifiedAt: String?,
    val rememberToken: String?,
    val isVerified: String?,
    var userType: UserHasType
)

data class ChangePassword(val oldPassword: String, val newPassword: String)
data class VerificationCode(val verificationCode: String)
