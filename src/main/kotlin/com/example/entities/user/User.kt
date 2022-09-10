package com.example.entities.user

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import com.example.models.user.body.JwtTokenBody
import com.example.controller.JwtController
import org.jetbrains.exposed.dao.id.EntityID

object UserTable : BaseIntIdTable("users") {
    val userName = varchar("user_name", 30)
    val email = varchar("email", 50)
    val password = varchar("password", 200)
    val mobileNumber = varchar("mobile_number", 50).nullable()
    val emailVerifiedAt = text("email_verified_at").nullable() // so far unkmown
    val rememberToken = varchar("remember_token", 50).nullable()
    val verificationCode = varchar("verification_code", 30).nullable() // verification_code
    val isVerified = bool("is_verified").nullable() // email verified by validation code
    override val primaryKey = PrimaryKey(id)
}

class UsersEntity(id: EntityID<String>) : BaseIntEntity(id, UserTable) {
    companion object : BaseIntEntityClass<UsersEntity>(UserTable)
    var userName by UserTable.userName
    var email by UserTable.email
    var password by UserTable.password
    var mobileNumber by UserTable.mobileNumber
    var emailVerifiedAt by UserTable.emailVerifiedAt
    var rememberToken by UserTable.rememberToken
    var verificationCode by UserTable.verificationCode
    var isVerified by UserTable.isVerified
    val userType by UserHasTypeEntity backReferencedOn UserHasTypeTable.userId
    fun response() = UsersResponse(
        id.value,
        userName,
        email,
        mobileNumber,
        emailVerifiedAt,
        rememberToken,
        isVerified,
        userType.userHasTypeResponse()
    )

    fun loggedInWithToken() = LoginResponse(
        response(), JwtController.tokenProvider(JwtTokenBody(id.value, email, userType.userTypeId))
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
data class LoginResponse(val user: UsersResponse?, val jwtToken: String)
data class ChangePassword(val oldPassword: String, val newPassword: String)
data class VerificationCode(val verificationCode: String)
