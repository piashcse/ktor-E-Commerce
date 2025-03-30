package com.piashcse.entities

import com.piashcse.controller.JwtController
import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import com.piashcse.models.user.body.JwtTokenRequest
import org.jetbrains.exposed.dao.id.EntityID

object UserTable : BaseIntIdTable("user") {
    val email = varchar("email", 50)
    val password = varchar("password", 200)
    val userType = varchar("user_type", 100)
    val mobileNumber = varchar("mobile_number", 50).nullable()
    val emailVerifiedAt = text("email_verified_at").nullable() // so far unkmown
    val rememberToken = varchar("remember_token", 50).nullable()
    val verificationCode = varchar("verification_code", 30).nullable() // verification_code
    val isVerified = bool("is_verified").nullable() // email verified by validation code
    override val primaryKey = PrimaryKey(id)
}

class UsersEntity(id: EntityID<String>) : BaseIntEntity(id, UserTable) {
    companion object : BaseIntEntityClass<UsersEntity>(UserTable)
    var email by UserTable.email
    var password by UserTable.password
    var userType by UserTable.userType
    var mobileNumber by UserTable.mobileNumber
    var emailVerifiedAt by UserTable.emailVerifiedAt
    var rememberToken by UserTable.rememberToken
    var verificationCode by UserTable.verificationCode
    var isVerified by UserTable.isVerified
    fun response() = UsersResponse(
        id.value,
        email,
        mobileNumber,
        emailVerifiedAt,
        rememberToken,
        isVerified,
        userType
    )

    fun loggedInWithToken() = LoginResponse(
        response(), JwtController.tokenProvider(JwtTokenRequest(id.value, email, userType))
    )
}

data class UsersResponse(
    val id: String,
    val email: String,
    val mobileNumber: String?,
    val emailVerifiedAt: String?,
    val rememberToken: String?,
    val isVerified: Boolean?,
    var userType: String
)
data class LoginResponse(val user: UsersResponse?, val accessToken: String)
data class ChangePassword(val oldPassword: String, val newPassword: String)
data class VerificationCode(val verificationCode: String)
