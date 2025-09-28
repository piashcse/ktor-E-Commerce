package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import com.piashcse.feature.auth.JwtConfig
import com.piashcse.model.request.JwtTokenRequest
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object UserTable : BaseIntIdTable("users") {
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
    companion object : BaseIntEntityClass<UserDAO>(UserTable, UserDAO::class.java)

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

    fun loggedInWithToken(refreshToken: String? = null) = LoginResponse(
        user = response(), 
        accessToken = JwtConfig.tokenProvider(JwtTokenRequest(id.value, email, userType)),
        refreshToken = refreshToken,
        tokenType = "Bearer",
        expiresIn = JwtConfig.ACCESS_TOKEN_VALIDITY_MS / 1000 // Convert to seconds
    )
}

data class UserResponse(
    val id: String,
    val email: String,
    val isVerified: Boolean?,
    var userType: String
)

data class LoginResponse(
    val user: UserResponse?,
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 1800 // 30 minutes in seconds (access token validity)
)
data class ChangePassword(val oldPassword: String, val newPassword: String)