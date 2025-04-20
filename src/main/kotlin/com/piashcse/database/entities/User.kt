package com.piashcse.database.entities

import com.piashcse.modules.auth.controller.JwtController
import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import com.piashcse.database.models.user.body.JwtTokenRequest
import org.jetbrains.exposed.dao.id.EntityID

object UserTable : BaseIntIdTable("user") {
    val email = varchar("email", 255) // Nullable for mobile users
    val userType = varchar("user_type", 100)
    val password = varchar("password", 200)
    val otpCode = varchar("otp_code", 6)
    val otpExpiry = varchar("otp_expiry", 50)
    val isVerified = bool("is_verified").default(false)
    override val primaryKey = PrimaryKey(id)

    // Create a composite unique index on email and userType
    init {
        uniqueIndex("email_userType_idx", email, userType)
    }
}

class UserDAO(id: EntityID<String>) : BaseIntEntity(id, UserTable) {
    companion object : BaseIntEntityClass<UserDAO>(UserTable)

    var email by UserTable.email
    var userType by UserTable.userType
    var password by UserTable.password
    var otpCode by UserTable.otpCode
    var otpExpiry by UserTable.otpExpiry
    var isVerified by UserTable.isVerified
    fun response() = UserResponse(
        id.value,
        email,
        isVerified,
        userType
    )

    fun loggedInWithToken() = LoginResponse(
        response(), JwtController.tokenProvider(JwtTokenRequest(id.value, email, userType))
    )
}

data class UserResponse(
    val id: String,
    val email: String,
    val isVerified: Boolean?,
    var userType: String
)

data class LoginResponse(val user: UserResponse?, val accessToken: String)
data class ChangePassword(val oldPassword: String, val newPassword: String)