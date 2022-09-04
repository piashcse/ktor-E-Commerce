package com.example.entities.user

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID
object UserTable : BaseIntIdTable("users") {
    val user_name = varchar("user_name", 30)
    val email = varchar("email", 20)
    val password = varchar("password", 20)
    val mobile_number = varchar("mobile_number", 20).nullable()
    val email_verified_at = text("email_verified_at").nullable() // so far unkmown
    val remember_token = varchar("remember_token", 50).nullable()
    val verification_code = varchar("verification_code", 20).nullable() // verification_code
    val is_verified = bool("is_verified").nullable() // email verified by validation code
    override val primaryKey = PrimaryKey(id)
}

class UsersEntity(id: EntityID<String>) :BaseIntEntity(id, UserTable) {
    companion object : BaseIntEntityClass<UsersEntity>(UserTable)
    var user_name by UserTable.user_name
    var email by UserTable.email
    var password by UserTable.password
    var mobile_number by UserTable.mobile_number
    var email_verified_at by UserTable.email_verified_at
    var remember_token by UserTable.remember_token
    var verification_code by UserTable.verification_code
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
    val isVerified: Boolean?,
    var userType: UserHasType
)

data class ChangePassword(val oldPassword: String, val newPassword: String)
data class VerificationCode(val verificationCode: String)
